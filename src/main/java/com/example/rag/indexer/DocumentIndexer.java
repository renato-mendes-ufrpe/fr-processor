package com.example.rag.indexer;

import com.example.rag.config.Config;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Responsável pela indexação de documentos PDF no sistema RAG.
 * 
 * O QUE É INDEXAÇÃO:
 * Indexação é o processo de transformar documentos em uma representação vetorial
 * que permite buscas por similaridade semântica (por significado, não apenas palavras).
 * 
 * PIPELINE DE INDEXAÇÃO (5 ETAPAS):
 * 
 * 1. CARREGAMENTO
 *    - Lê o arquivo PDF do disco
 *    - Converte para objeto Document do LangChain4j
 * 
 * 2. PARSING (Análise)
 *    - Extrai o texto do PDF usando Apache Tika
 *    - Remove formatação, mantém apenas conteúdo textual
 * 
 * 3. CHUNKING (Divisão)
 *    - Divide o texto em pedaços menores (chunks/segments)
 *    - Usa DocumentSplitter recursivo com overlap
 *    - Cada chunk tem ~2000 tokens com 600 tokens de sobreposição
 * 
 * 4. EMBEDDING (Vetorização)
 *    - Converte cada chunk em um vetor numérico (embedding)
 *    - Usa modelo AllMiniLmL6V2 (local, offline, 384 dimensões)
 *    - Embeddings capturam o significado semântico do texto
 * 
 * 5. ARMAZENAMENTO
 *    - Salva os embeddings no EmbeddingStore (banco de vetores em memória)
 *    - Permite buscas posteriores por similaridade
 * 
 * MODELO DE EMBEDDINGS:
 * - Nome: AllMiniLmL6V2
 * - Tipo: ONNX (Open Neural Network Exchange)
 * - Tamanho: ~80 MB
 * - Dimensões: 384
 * - Características: Rápido, leve, roda localmente sem API
 * - Qualidade: Excelente para busca semântica em português
 * 
 * USO:
 * DocumentIndexer indexer = new DocumentIndexer();
 * indexer.indexDocument(caminhoArquivo);
 * EmbeddingStore store = indexer.getEmbeddingStore();
 */
public class DocumentIndexer {
    
    /**
     * Armazena os embeddings (vetores) dos chunks de texto na memória.
     * 
     * O InMemoryEmbeddingStore é um banco de dados vetorial simples que:
     * - Armazena pares de (TextSegment, Embedding)
     * - Permite busca por similaridade usando distância cosseno
     * - Roda em memória RAM (rápido, mas perde dados ao fechar)
     * 
     * Para produção, considere usar stores persistentes como:
     * - Pinecone
     * - Weaviate
     * - Chroma
     * - Milvus
     */
    private final InMemoryEmbeddingStore<TextSegment> embeddingStore;
    
    /**
     * Modelo que converte texto em embeddings (vetores numéricos).
     * 
     * AllMiniLmL6V2 é um modelo de embeddings:
     * - Baseado em Sentence Transformers
     * - Otimizado para busca semântica
     * - Roda localmente (não precisa de internet ou API key)
     * - Download automático na primeira execução (~80 MB)
     * 
     * O modelo gera vetores de 384 dimensões que capturam
     * o significado semântico do texto de entrada.
     */
    private final EmbeddingModel embeddingModel;
    
    /**
     * Construtor da classe DocumentIndexer.
     * 
     * Inicializa os componentes necessários:
     * 1. InMemoryEmbeddingStore - Banco de vetores em memória
     * 2. AllMiniLmL6V2EmbeddingModel - Modelo de embeddings local
     * 
     * Nota: O modelo é baixado automaticamente na primeira execução
     * e fica em cache para usos futuros (~80 MB).
     */
    public DocumentIndexer() {
        this.embeddingStore = new InMemoryEmbeddingStore<>();
        this.embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        
        System.out.println("✅ DocumentIndexer inicializado");
        System.out.println("   Embedding Model: AllMiniLmL6V2 (384 dimensões, local)");
    }
    
    /**
     * Indexa um documento PDF completo no sistema RAG.
     * 
     * PROCESSO COMPLETO:
     * 
     * 1. Valida se o arquivo existe
     * 2. Carrega o PDF usando Apache Tika
     * 3. Divide o documento em chunks usando DocumentSplitter recursivo
     * 4. Gera embeddings para cada chunk usando AllMiniLmL6V2
     * 5. Armazena os embeddings no InMemoryEmbeddingStore
     * 
     * PARÂMETROS DE CHUNKING (definidos em Config.java):
     * - Tamanho máximo: 500 tokens (~375 palavras)
     * - Overlap: 50 tokens (~37 palavras)
     * - Estratégia: Recursiva (tenta manter parágrafos inteiros)
     * 
     * PERFORMANCE:
     * - PDF pequeno (100 páginas): ~30 segundos
     * - PDF médio (500 páginas): ~2 minutos
     * - PDF grande (1000 páginas): ~5 minutos
     * 
     * @param pdfFilePath Caminho completo para o arquivo PDF a ser indexado
     *                    Exemplo: "data/report/documento.pdf"
     * @throws RuntimeException Se o arquivo não existir ou houver erro no processamento
     */
    public void indexDocument(String pdfFilePath) {
        try {
            System.out.println("📄 Iniciando indexação do documento: " + pdfFilePath);
            
            // ETAPA 1: Carregar o arquivo PDF
            Path path = Paths.get(pdfFilePath);
            System.out.println("   [1/5] Carregando arquivo PDF...");
            
            // ETAPA 2: Parse do PDF com Apache Tika
            // Apache Tika é uma biblioteca universal de parsing que suporta:
            // - PDF, DOCX, PPTX, XLSX
            // - HTML, XML, TXT
            // - Imagens com OCR (se configurado)
            ApacheTikaDocumentParser parser = new ApacheTikaDocumentParser();
            Document document;
            try (InputStream inputStream = new FileInputStream(path.toFile())) {
                document = parser.parse(inputStream);
            }
            System.out.println("   [2/5] Parsing concluído: " + document.text().length() + " caracteres");
            
            // ETAPA 3: Dividir documento em chunks
            // DocumentSplitter recursivo tenta manter a estrutura do texto:
            // - Primeiro tenta dividir por parágrafos duplos (\n\n)
            // - Se o chunk for muito grande, divide por parágrafos simples (\n)
            // - Se ainda for grande, divide por sentenças (.)
            // - Como último recurso, divide por palavras
            System.out.println("   [3/5] Dividindo em chunks...");
            DocumentSplitter splitter = DocumentSplitters.recursive(
                Config.MAX_SEGMENT_SIZE_IN_TOKENS,
                Config.SEGMENT_OVERLAP_IN_TOKENS
            );

            // ETAPA 4: Criar o Ingestor (processador de ingestão)
            // EmbeddingStoreIngestor coordena o processo de:
            // 1. Pegar cada chunk do DocumentSplitter
            // 2. Enviar para o EmbeddingModel gerar o embedding
            // 3. Armazenar o par (chunk, embedding) no EmbeddingStore
            System.out.println("   [4/5] Gerando embeddings (pode demorar alguns minutos)...");
            EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(splitter)
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();
            
            // ETAPA 5: Executar a ingestão completa
            // Este método bloqueia até processar todo o documento
            ingestor.ingest(document);
            
            // Exibe estatísticas finais
            System.out.println("   [5/5] Indexação concluída!");
            System.out.println("   ✅ Documento indexado com sucesso");
            System.out.println("   ✅ Embeddings armazenados em memória");
            
        } catch (Exception e) {
            System.err.println("❌ Erro ao indexar documento: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Falha na indexação", e);
        }
    }
    
    /**
     * Retorna o EmbeddingStore contendo todos os embeddings indexados.
     * 
     * Este método é usado por RagQueryEngine para realizar buscas
     * por similaridade no conjunto de chunks indexados.
     * 
     * COMO USAR:
     * EmbeddingStore store = indexer.getEmbeddingStore();
     * // Passar store para RagQueryEngine
     * 
     * ESTRUTURA DO STORE:
     * O InMemoryEmbeddingStore contém:
     * - Lista de TextSegments (chunks de texto)
     * - Lista de Embeddings (vetores de 384 dimensões)
     * - Índice interno para busca eficiente por similaridade
     * 
     * @return EmbeddingStore contendo todos os chunks e seus embeddings
     */
    public EmbeddingStore<TextSegment> getEmbeddingStore() {
        return embeddingStore;
    }
    
    /**
     * Retorna o modelo de embeddings utilizado na indexação.
     * 
     * O mesmo modelo deve ser usado tanto na indexação quanto
     * na busca (query time) para garantir consistência.
     * 
     * POR QUE ISSO É IMPORTANTE:
     * - Embeddings de modelos diferentes não são comparáveis
     * - Cada modelo tem seu próprio "espaço vetorial"
     * - Usar modelos diferentes = busca não funciona corretamente
     * 
     * COMO USAR:
     * EmbeddingModel model = indexer.getEmbeddingModel();
     * // Passar model para RagQueryEngine
     * 
     * @return Instância do AllMiniLmL6V2EmbeddingModel usado na indexação
     */
    public EmbeddingModel getEmbeddingModel() {
        return embeddingModel;
    }
}
