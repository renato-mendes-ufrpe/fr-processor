package com.example.rag.retrieval;

import com.example.rag.config.Config;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Motor de consulta RAG (Retrieval-Augmented Generation).
 * 
 * Responsável pelas fases de RETRIEVAL e GENERATION do pipeline RAG.
 * 
 * O QUE ESTE MOTOR FAZ:
 * 
 * ETAPA 1: RETRIEVAL (Recuperação)
 * - Recebe uma pergunta do usuário em linguagem natural
 * - Converte a pergunta em embedding (vetor de 384 dimensões)
 * - Busca por similaridade cosseno no EmbeddingStore
 * - Retorna os chunks mais relevantes com seus scores de similaridade
 * 
 * ETAPA 2: GENERATION (Geração - Opcional)
 * - Constrói um contexto aumentado com os chunks encontrados
 * - Cria um prompt especial incluindo contexto + pergunta
 * - Envia para o Google Gemini (se configurado)
 * - Retorna a resposta gerada pelo modelo
 * 
 * BUSCA POR SIMILARIDADE:
 * Usa distância cosseno para encontrar os embeddings mais próximos.
 * 
 * Exemplo de scores:
 * - 1.0 = Idêntico (impossível para textos diferentes)
 * - 0.9-1.0 = Extremamente similar
 * - 0.8-0.9 = Muito similar (geralmente relevante)
 * - 0.7-0.8 = Similar (pode ser relevante)
 * - Abaixo de 0.7 = Menos similar (configurável)
 * 
 * MODOS DE OPERAÇÃO:
 * 
 * 1. Modo Completo (com Gemini configurado):
 *    query("pergunta") → retorna resposta gerada pelo Gemini
 * 
 * 2. Modo Somente Retrieval (sem Gemini):
 *    query("pergunta") → retorna contexto recuperado (sem geração)
 *    retrieveOnly("pergunta") → retorna lista de matches
 * 
 * CONFIGURAÇÃO DO GEMINI:
 * - Model: gemini-2.5-flash (configurável via .env)
 * - Temperature: 0.7 (equilíbrio entre criatividade e consistência)
 * - Max Retries: 3 tentativas em caso de erro
 * - Timeout: 30 segundos por chamada
 * 
 * USO BÁSICO:
 * EmbeddingStore store = indexer.getEmbeddingStore();
 * EmbeddingModel model = indexer.getEmbeddingModel();
 * RagQueryEngine engine = new RagQueryEngine(store, model);
 * String resposta = engine.query("Qual o principal negócio da empresa?");
 */
public class RagQueryEngine {
    
    /**
     * Armazena os embeddings (vetores) indexados na fase de indexação.
     * 
     * O EmbeddingStore funciona como um banco de dados vetorial que permite
     * buscar chunks de texto por similaridade semântica.
     * 
     * Este é o mesmo store criado e populado pelo DocumentIndexer.
     */
    private final EmbeddingStore<TextSegment> embeddingStore;
    
    /**
     * Modelo que converte texto em embeddings (vetores).
     * 
     * IMPORTANTE: Deve ser o MESMO modelo usado na indexação!
     * 
     * Se usar modelos diferentes:
     * - Na indexação: AllMiniLmL6V2
     * - Na query: Outro modelo
     * = Busca não funcionará (vetores incomparáveis)
     * 
     * Modelo atual: AllMiniLmL6V2 (local, 384 dimensões)
     */
    private final EmbeddingModel embeddingModel;
    
    /**
     * Modelo de linguagem (LLM) usado para gerar respostas.
     * 
     * Configurado apenas se GEMINI_API_KEY estiver presente no .env
     * 
     * Se null:
     * - Sistema opera em modo "somente retrieval"
     * - Retorna apenas o contexto recuperado, sem geração
     * 
     * Se configurado:
     * - GoogleAiGeminiChatModel com gemini-2.5-flash
     * - Gera respostas baseadas no contexto recuperado
     */
    private final ChatModel chatModel;
    
    /**
     * Construtor do motor de consulta RAG.
     * 
     * Inicializa o motor com os componentes necessários:
     * 1. EmbeddingStore - Banco de vetores com chunks indexados
     * 2. EmbeddingModel - Modelo para converter queries em embeddings
     * 3. ChatModel - (Opcional) Gemini para geração de respostas
     * 
     * O Gemini é inicializado automaticamente se:
     * - Arquivo .env existe
     * - GEMINI_API_KEY está configurada
     * - Chave não está vazia ou placeholder
     * 
     * PARÂMETROS DO GEMINI:
     * - apiKey: Lida do .env
     * - modelName: gemini-2.5-flash (configurável)
     * - temperature: 0.7 (criatividade moderada)
     * - maxRetries: 3 (tentativas em caso de erro)
     * - timeout: 30 segundos
     * 
     * @param embeddingStore Store contendo todos os chunks indexados
     * @param embeddingModel Modelo de embeddings (deve ser o mesmo da indexação)
     */
    public RagQueryEngine(EmbeddingStore<TextSegment> embeddingStore, 
                          EmbeddingModel embeddingModel) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
        
        // Inicializa Gemini se estiver configurado
        if (Config.isGeminiConfigured()) {
            this.chatModel = GoogleAiGeminiChatModel.builder()
                    .apiKey(Config.GEMINI_API_KEY)
                    .modelName(Config.GEMINI_MODEL)
                    .temperature(0.0) // Determinístico
                    .maxRetries(3)
                    .timeout(Duration.ofSeconds(30))
                    .build();
            System.out.println("✅ RagQueryEngine inicializado com Gemini (" + Config.GEMINI_MODEL + ", temperature=0.0)");
        } else {
            this.chatModel = null;
            System.out.println("✅ RagQueryEngine inicializado (somente retrieval - sem Gemini)");
        }
    }
    
    /**
     * Executa uma consulta RAG completa: recuperação + geração (opcional).
     * 
     * FLUXO DE EXECUÇÃO:
     * 
     * 1. VETORIZAÇÃO DA QUERY
     *    - Converte a pergunta do usuário em embedding
     *    - Usa o mesmo modelo da indexação (AllMiniLmL6V2)
     * 
     * 2. BUSCA POR SIMILARIDADE
     *    - Compara o embedding da query com todos os chunks indexados
     *    - Usa distância cosseno (quanto mais próximo de 1.0, mais similar)
     *    - Filtra por score mínimo (0.7 por padrão)
     *    - Retorna até 5 chunks mais relevantes
     * 
     * 3. CONSTRUÇÃO DO CONTEXTO
     *    - Concatena os chunks recuperados
     *    - Separa com marcadores "---"
     * 
     * 4. CRIAÇÃO DO PROMPT AUMENTADO
     *    - Instrução para o LLM: "Use apenas estas informações"
     *    - Contexto: Chunks recuperados
     *    - Pergunta: Query do usuário
     * 
     * 5. GERAÇÃO (SE GEMINI CONFIGURADO)
     *    - Envia prompt para o Gemini
     *    - Retorna resposta gerada
     *    - Em caso de erro, retorna o prompt sem resposta
     * 
     * 6. RETORNO (SE GEMINI NÃO CONFIGURADO)
     *    - Retorna apenas o prompt aumentado
     *    - Útil para testar retrieval ou usar outro LLM
     * 
     * EXEMPLO DE USO:
     * String resposta = engine.query("Qual o principal negócio da empresa?");
     * System.out.println(resposta);
     * 
     * EXEMPLO DE SAÍDA (com Gemini):
     * "A Ambipar é especializada em gestão de resíduos e emergências ambientais..."
     * 
     * EXEMPLO DE SAÍDA (sem Gemini):
     * "Você é um assistente...
     *  DOCUMENTOS:
     *  [contexto recuperado]
     *  PERGUNTA: Qual o principal negócio da empresa?"
     * 
     * @param userQuestion Pergunta do usuário em linguagem natural
     * @return Resposta gerada (com Gemini) ou prompt aumentado (sem Gemini)
     */
    public String query(String userQuestion) {
        System.out.println("\n🔍 Processando query: \"" + userQuestion + "\"");
        
        // 1. Converter a pergunta em embedding
        System.out.println("   🔄 Gerando embedding da query...");
        Embedding queryEmbedding = embeddingModel.embed(userQuestion).content();
        
        // 2. Buscar documentos similares
        System.out.println("   🔎 Buscando documentos relevantes...");
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(Config.MAX_RESULTS_FOR_RETRIEVAL)
                .minScore(Config.MIN_SCORE_FOR_RETRIEVAL)
                .build();
        
        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
        List<EmbeddingMatch<TextSegment>> matches = searchResult.matches();
        
        System.out.println("   ✅ Encontrados " + matches.size() + " documentos relevantes");
        
        // 3. Imprimir os matches
        printMatches(matches);
        
        // 4. Construir contexto aumentado
        String context = buildContext(matches);
        
        // 5. Criar prompt aumentado
        String augmentedPrompt = buildAugmentedPrompt(userQuestion, context);
        
        System.out.println("\n   💡 Contexto recuperado com sucesso!");
        System.out.println("   📊 Total de caracteres no contexto: " + context.length());
        
        // 6. Se Gemini estiver configurado, gerar resposta
        if (chatModel != null) {
            System.out.println("   🤖 Enviando para Gemini...");
            try {
                String answer = chatModel.chat(augmentedPrompt);
                System.out.println("   ✅ Resposta recebida do Gemini");
                return answer;
            } catch (Exception e) {
                // Log detalhado da exceção, mas NÃO retornar o prompt como resposta.
                // Retornar null permite que o QuestionProcessor trate como "INFORMAÇÃO NÃO ENCONTRADA".
                System.err.println("   ❌ Erro ao chamar Gemini: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                e.printStackTrace(System.err);
                return null;
            }
        } else {
            // Sem Gemini, retorna apenas o prompt aumentado
            return augmentedPrompt;
        }
    }
    
    /**
     * Executa busca somente por retrieval, sem geração de resposta.
     * 
     * Este método é útil para:
     * - Testar a qualidade da recuperação de documentos
     * - Debug: ver quais chunks estão sendo encontrados
     * - Usar o contexto recuperado em outro LLM
     * - Implementar lógica customizada de geração
     * 
     * DIFERENÇA DO MÉTODO query():
     * - query(): Retrieval + Geração (com Gemini, se configurado)
     * - retrieveOnly(): Apenas retrieval (retorna matches brutos)
     * 
     * RETORNO:
     * Lista de EmbeddingMatch contendo:
     * - embedded: TextSegment com o texto do chunk
     * - score: Double com similaridade (0.0 a 1.0)
     * - embeddingId: ID único do embedding
     * 
     * EXEMPLO DE USO:
     * List matches = engine.retrieveOnly("Qual o principal negócio?");
     * for (EmbeddingMatch match : matches) {
     *     System.out.println("Score: " + match.score());
     *     System.out.println("Texto: " + match.embedded().text());
     * }
     * 
     * @param userQuestion Pergunta do usuário em linguagem natural
     * @return Lista de matches ordenados por similaridade (maior para menor)
     */
    public List<EmbeddingMatch<TextSegment>> retrieveOnly(String userQuestion) {
        System.out.println("\n🔍 Modo Retrieval Only: \"" + userQuestion + "\"");
        
        Embedding queryEmbedding = embeddingModel.embed(userQuestion).content();
        
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(Config.MAX_RESULTS_FOR_RETRIEVAL)
                .minScore(Config.MIN_SCORE_FOR_RETRIEVAL)
                .build();
        
        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
        List<EmbeddingMatch<TextSegment>> matches = searchResult.matches();
        
        System.out.println("   ✅ Encontrados " + matches.size() + " documentos");
        printMatches(matches);
        
        return matches;
    }
    
    /**
     * Constrói o contexto aumentado a partir dos chunks recuperados.
     * 
     * Pega todos os TextSegments dos matches e concatena em uma string única.
     * Cada chunk é separado por "---" para facilitar a leitura.
     * 
     * Se não houver matches (nenhum documento relevante encontrado),
     * retorna uma mensagem informando isso.
     * 
     * FORMATO DO CONTEXTO:
     * 
     * [Texto do chunk 1]
     * 
     * ---
     * 
     * [Texto do chunk 2]
     * 
     * ---
     * 
     * [Texto do chunk 3]
     * 
     * @param matches Lista de matches retornados pela busca de similaridade
     * @return String contendo o contexto concatenado ou mensagem de "nenhum documento encontrado"
     */
    private String buildContext(List<EmbeddingMatch<TextSegment>> matches) {
        if (matches.isEmpty()) {
            return "Nenhum documento relevante foi encontrado.";
        }
        
        return matches.stream()
                .map(match -> match.embedded().text())
                .collect(Collectors.joining("\n\n---\n\n"));
    }
    
    /**
     * Constrói o prompt aumentado para envio ao LLM.
     * 
     * Este prompt segue o padrão RAG:
     * 1. Instruções ao modelo (papel, comportamento esperado)
     * 2. Contexto recuperado (chunks relevantes)
     * 3. Pergunta do usuário
     * 4. Marcador de resposta
     * 
     * INSTRUÇÕES AO MODELO:
     * - Você é um assistente especializado
     * - Use APENAS as informações fornecidas nos documentos
     * - Se não souber, admita ("não tenho informação suficiente")
     * - Não invente informações (previne alucinações)
     * 
     * POR QUE ISSO PREVINE ALUCINAÇÕES:
     * - O LLM é explicitamente instruído a usar apenas o contexto
     * - Se o contexto não contém a resposta, o modelo deve admitir
     * - Reduz significativamente respostas inventadas
     * 
     * FORMATO DO PROMPT:
     * ```
     * Você é um assistente especializado em análise de relatórios...
     * 
     * Use as seguintes informações dos documentos para responder...
     * Se a resposta não puder ser encontrada nos documentos, diga...
     * 
     * DOCUMENTOS:
     * [contexto]
     * 
     * PERGUNTA DO USUÁRIO:
     * [pergunta]
     * 
     * RESPOSTA:
     * ```
     * 
     * Este prompt pode ser enviado para qualquer LLM (GPT, Claude, Gemini, Llama, etc)
     * 
     * @param userQuestion Pergunta original do usuário
     * @param context Contexto recuperado (chunks concatenados)
     * @return Prompt completo formatado para o LLM
     */
    private String buildAugmentedPrompt(String userQuestion, String context) {
        return String.format("""
                Você é um assistente especializado em análise de relatórios financeiros e empresariais.
                
                Use as seguintes informações dos documentos para responder a pergunta do usuário.
                Se a resposta não puder ser encontrada nos documentos fornecidos, diga que não tem 
                informação suficiente para responder.
                
                DOCUMENTOS:
                %s
                
                PERGUNTA DO USUÁRIO:
                %s
                
                RESPOSTA:
                """, context, userQuestion);
    }
    
    /**
     * Exibe no console os chunks recuperados com seus scores de similaridade.
     * 
     * Para cada match, mostra:
     * - Número do resultado (1, 2, 3...)
     * - Score de similaridade (0.0 a 1.0)
     * - Preview do texto (primeiros 150 caracteres)
     * 
     * INTERPRETAÇÃO DOS SCORES:
     * - 0.90-1.00: Extremamente relevante
     * - 0.80-0.90: Muito relevante
     * - 0.70-0.80: Relevante (threshold padrão: 0.7)
     * - Abaixo de 0.70: Pouco relevante (filtrado)
     * 
     * FORMATO DA SAÍDA:
     * 
     *    📄 Documentos recuperados:
     *       [1] Score: 0.8466 | Preview: Os principais mecanismos...
     *       [2] Score: 0.8360 | Preview: Principais insumos e...
     *       [3] Score: 0.8356 | Preview: Em relação ao último...
     * 
     * Os newlines (\n) no texto são substituídos por espaços para melhor visualização.
     * 
     * @param matches Lista de matches com chunks e scores
     */
    private void printMatches(List<EmbeddingMatch<TextSegment>> matches) {
        System.out.println("\n   📄 Documentos recuperados:");
        for (int i = 0; i < matches.size(); i++) {
            EmbeddingMatch<TextSegment> match = matches.get(i);
            String preview = match.embedded().text().substring(0, 
                    Math.min(150, match.embedded().text().length()));
            System.out.printf("      [%d] Score: %.4f | Preview: %s...%n", 
                    i + 1, match.score(), preview.replace("\n", " "));
        }
        System.out.println();
    }
}
