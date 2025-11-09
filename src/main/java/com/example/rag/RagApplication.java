package com.example.rag;

import com.example.rag.automation.CsvQuestionReader;
import com.example.rag.automation.QuestionProcessor;
import com.example.rag.automation.model.CompanyResponse;
import com.example.rag.automation.model.Question;
import com.example.rag.config.Config;
import com.example.rag.indexer.DocumentIndexer;
import com.example.rag.retrieval.RagQueryEngine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Sistema de Processamento Automatizado de Formulários de Referência (FR).
 * 
 * Utiliza RAG (Retrieval-Augmented Generation) para extrair informações específicas
 * de Formulários de Referência corporativos, seguindo um guia de coleta predefinido.
 * 
 * FLUXO COMPLETO:
 * 
 * FASE 1: INDEXAÇÃO
 * ├── 1.1 Carregar arquivo PDF do Formulário de Referência
 * ├── 1.2 Extrair texto (Apache Tika)
 * ├── 1.3 Dividir em chunks (1200 tokens, overlap 200)
 * ├── 1.4 Gerar embeddings para cada chunk (AllMiniLmL6V2)
 * └── 1.5 Armazenar em banco vetorial em memória
 * 
 * FASE 2: PROCESSAMENTO DE QUESTÕES
 * ├── 2.1 Ler questões do arquivo "Guia de Coleta.csv"
 * ├── 2.2 Para cada questão:
 * │   ├── Enriquecer query com termos do guia
 * │   ├── Buscar chunks relevantes via RAG
 * │   ├── Construir prompt estruturado
 * │   ├── Enviar para Google Gemini
 * │   └── Pós-processar resposta (formatação, multiplicação monetária)
 * └── 2.3 Salvar respostas em output/respostas.csv
 * 
 * QUESTÕES PROCESSADAS (24 questões):
 * 1. Receita líquida da empresa
 * 2. Lucro líquido da empresa
 * 3. Firma de auditoria independente
 * 4. Gastos anuais com auditoria
 * 5. Gastos com serviços adicionais de auditoria
 * 6. Política de gerenciamento de riscos
 * 7. Existência de auditoria interna
 * 8. Sistema de controles internos adequado
 * 9. Deficiências/recomendações sobre controles internos
 * 10. Divulgação de informações ASG
 * 11. Conselho Fiscal instalado
 * 12. Quantidade de Comitês do Conselho de Administração
 * 13. Regras de conflitos de interesses
 * 14. Membros do Conselho de Administração
 * 15. Mulheres no Conselho de Administração
 * 16. Conselheiros externos
 * 17. Conselheiros independentes
 * 18. Conselheiros executivos
 * 19. Membros do Comitê de Auditoria
 * 20. Membros do Comitê de Auditoria que são conselheiros
 * 21. Membros do Comitê de Auditoria que são conselheiros independentes
 * 22. Comitê de Auditoria coordenado por conselheiro independente
 * 23. Seguro D&O
 * 24. Casos de desvios/fraudes
 * 
 * REQUISITOS:
 * - Java 21 ou superior
 * - Formulário de Referência em PDF (data/report/)
 * - Arquivo "Guia de Coleta.csv" na raiz do projeto
 * - GEMINI_API_KEY configurada no arquivo .env
 * - Internet na primeira execução (download de modelo de embeddings ~80MB)
 * 
 * ARQUIVOS:
 * - Input: data/report/*.pdf + Guia de Coleta.csv
 * - Output: output/respostas.csv
 * 
 * PERFORMANCE:
 * - Indexação: ~10 segundos para 200 páginas (chunks maiores)
 * - Processamento: ~8 segundos por questão (RAG + Gemini)
 * - Total: ~200 segundos (~3min20s) para 24 questões
 */
public class RagApplication {
    
    /**
     * Método principal da aplicação.
     * 
     * Executa o fluxo completo de processamento:
     * 1. Exibe banner
     * 2. Carrega configurações
     * 3. Indexa documento PDF (Formulário de Referência)
     * 4. Processa questões do CSV automaticamente
     * 5. Gera arquivo CSV com respostas
     * 
     * @param args Argumentos de linha de comando (não utilizados)
     */
    public static void main(String[] args) {
        printBanner();
        
        // Mostrar configurações
        Config.printConfig();
        
        try {
            // FASE 1: INDEXING
            System.out.println("\n" + "=".repeat(80));
            System.out.println("📚 FASE 1: INDEXING - Carregando e indexando documentos");
            System.out.println("=".repeat(80));
            
            DocumentIndexer indexer = new DocumentIndexer();
            String pdfPath = Config.DATA_FOLDER + "/" + Config.AMBIPAR_PDF_FILE;
            indexer.indexDocument(pdfPath);
            
            // FASE 2: RETRIEVAL & QUERY
            System.out.println("\n" + "=".repeat(80));
            System.out.println("🔍 FASE 2: RETRIEVAL & QUERY - Consultando documentos");
            System.out.println("=".repeat(80));
            
            RagQueryEngine queryEngine = new RagQueryEngine(
                    indexer.getEmbeddingStore(),
                    indexer.getEmbeddingModel()
            );
            
            // Processar questões do CSV
            runCsvQuestionMode(queryEngine);
            
        } catch (Exception e) {
            System.err.println("\n❌ Erro na execução: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n✅ Aplicação finalizada!");
    }
    
    /**
     * Processa questões do arquivo CSV usando o guia de coleta.
     * 
     * FLUXO:
     * 1. Lê o CSV com CsvQuestionReader
     * 2. Processa com QuestionProcessor (RAG + LLM)
     * 3. Salva respostas em output/respostas.csv
     * 
     * @param queryEngine Motor de consulta RAG já inicializado
     */
    private static void runCsvQuestionMode(RagQueryEngine queryEngine) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📊 MODO PROCESSAMENTO CSV - 24 Questões do FR");
        System.out.println("=".repeat(80));
        
        String csvPath = "Guia de Coleta.csv";
        
        // Verificar se arquivo existe
        if (!Files.exists(Path.of(csvPath))) {
            System.err.println("❌ Arquivo não encontrado: " + csvPath);
            System.err.println("   Esperado na raiz do projeto.");
            return;
        }
        
        try {
            // Configuração
            String companyName = "Ambipar Participações e Empreendimentos S.A.";
            
            // MODO COMPLETO: Processar todas as 24 questões
            // Para debug: altere para 5 (Q2, Q3, Q5, Q6, Q8)
            int numQuestionsToProcess = 24;  // TODAS as questões
            
            System.out.println("🏢 Empresa: " + companyName);
            System.out.println("📋 Questões a processar: " + numQuestionsToProcess + " primeiras do CSV");
            
            // Inicializar componentes
            CsvQuestionReader reader = new CsvQuestionReader();
            QuestionProcessor processor = new QuestionProcessor(queryEngine);
            
            // Criar objeto de resposta
            CompanyResponse response = new CompanyResponse(companyName);
            
            // Preparar path de output
            Path outputPath = Path.of("output/respostas.csv");
            Files.createDirectories(outputPath.getParent());
            
            // Processar cada questão por índice (sequência no CSV)
            for (int index = 0; index < numQuestionsToProcess; index++) {
                System.out.println("\n" + "━".repeat(80));
                System.out.println("📝 Processando questão " + (index + 1) + " de " + numQuestionsToProcess);
                System.out.println("━".repeat(80));
                
                // Ler questão do CSV por índice
                Question question = reader.readQuestionByIndex(index);
                
                if (question == null) {
                    System.err.println("⚠️ Questão no índice " + index + " não encontrada no CSV");
                    continue;
                }
                
                System.out.println("   Questão Nº: " + question.getNumero());
                System.out.println("   Texto: " + question.getQuestao());
                
                try {
                    // Processar questão
                    String answer = processor.processQuestion(question);
                    
                    // Armazenar resposta usando o número da questão
                    response.setResposta(question.getNumero(), answer);
                    
                    System.out.println("✅ Resposta: " + 
                            (answer.length() > 100 ? answer.substring(0, 100) + "..." : answer));
                    
                } catch (Exception e) {
                    System.err.println("❌ Erro ao processar questão " + question.getNumero() + ": " + e.getMessage());
                    response.setResposta(question.getNumero(), "ERRO: " + e.getMessage());
                }
                
                // CHECKPOINT: Salvar progresso periodicamente
                if ((index + 1) % Config.CHECKPOINT_INTERVAL == 0) {
                    System.out.println("\n💾 Checkpoint: Salvando progresso (" + (index + 1) + " questões processadas)...");
                    saveProgressToFile(response, outputPath);
                }
                
                // RATE LIMITING: Delay entre requisições para não exceder limites da API
                if (index < numQuestionsToProcess - 1) {
                    long delayMs = Config.REQUEST_DELAY_MS;
                    System.out.println("⏳ Aguardando " + (delayMs/1000.0) + "s antes da próxima questão (rate limiting)...");
                    Thread.sleep(delayMs);
                }
            }
            
            // Salvar resultado final
            System.out.println("\n" + "=".repeat(80));
            System.out.println("💾 Salvando resultados finais...");
            System.out.println("=".repeat(80));
            
            saveProgressToFile(response, outputPath);
            
            System.out.println("\n✅ Resposta salva em: " + outputPath.toAbsolutePath());
            
            // Exibir conteúdo do arquivo
            System.out.println("\n📄 Conteúdo do arquivo CSV:");
            System.out.println("─".repeat(80));
            Files.lines(outputPath).forEach(System.out::println);
            System.out.println("─".repeat(80));
            
            // Resumo
            System.out.println("\n📊 RESUMO DO PROCESSAMENTO:");
            System.out.println("   • Empresa: " + companyName);
            System.out.println("   • Questões processadas: " + numQuestionsToProcess);
            System.out.println("   • Arquivo gerado: " + outputPath.getFileName());
            
        } catch (IOException e) {
            System.err.println("❌ Erro ao processar CSV: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("❌ Erro: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Salva o progresso atual em arquivo CSV com codificação UTF-8 BOM.
     * 
     * Este método é usado tanto para checkpoints quanto para salvar o resultado final.
     * Sobrescreve o arquivo existente com o estado atual das respostas.
     * 
     * UTF-8 com BOM é necessário para que o Excel no macOS/Windows exiba corretamente
     * caracteres especiais (acentos, cedilha, etc).
     * 
     * @param response Objeto com as respostas coletadas até o momento
     * @param outputPath Caminho do arquivo CSV de saída
     * @throws IOException Se houver erro ao escrever o arquivo
     */
    private static void saveProgressToFile(CompanyResponse response, Path outputPath) throws IOException {
        // BOM (Byte Order Mark) para UTF-8 - indica ao Excel que o arquivo é UTF-8
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        
        // Criar conteúdo completo do CSV
        String content = CompanyResponse.csvHeader() + "\n" + response.toCsvLine() + "\n";
        
        // Escrever BOM + conteúdo em UTF-8
        java.io.FileOutputStream fos = new java.io.FileOutputStream(outputPath.toFile());
        fos.write(bom);
        fos.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        fos.close();
    }
    
    /**
     * Exibe o banner de boas-vindas da aplicação.
     */
    private static void printBanner() {
        System.out.println("\n");
        System.out.println("╔═══════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                                               ║");
        System.out.println("║              PROCESSAMENTO AUTOMATIZADO DE FORMULÁRIOS DE REFERÊNCIA          ║");
        System.out.println("║                          RAG + LLM para Análise Corporativa                   ║");
        System.out.println("║                                                                               ║");
        System.out.println("║  Extração automatizada de informações de FRs usando:                          ║");
        System.out.println("║  • LangChain4j - Framework Java para LLMs                                     ║");
        System.out.println("║  • Google Gemini - Geração de respostas                                       ║");
        System.out.println("║  • AllMiniLmL6V2 - Embeddings locais (384 dim)                               ║");
        System.out.println("║  • Apache Tika - Extração de texto de PDFs                                    ║");
        System.out.println("║                                                                               ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════════════════╝");
    }
}
