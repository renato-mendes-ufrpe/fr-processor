package com.example.rag;

import com.example.rag.config.Config;
import com.example.rag.indexer.DocumentIndexer;
import com.example.rag.retrieval.RagQueryEngine;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import com.example.rag.automation.QuestionProcessor;
import com.example.rag.automation.CsvQuestionReader;
import com.example.rag.automation.model.CompanyResponse;
import com.example.rag.automation.model.Question;

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
 * ├── 1.3 Dividir em chunks (2000 tokens, overlap 600)
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
     * Executa o fluxo completo conforme descrito na documentação da classe:
     * 
     * FASE 1: INDEXAÇÃO
     * 1.1 Exibe banner
     * 1.2 Carrega configurações
     * 1.3 Lista e seleciona arquivos PDF do Formulário de Referência
     * 1.4 Para cada PDF:
     *     - Extrai texto (Apache Tika)
     *     - Divide em chunks (segmentação)
     *     - Gera embeddings para cada chunk
     *     - Armazena em banco vetorial em memória
     * 
     * FASE 2: PROCESSAMENTO DE QUESTÕES
     * 2.1 Lê questões do arquivo "Guia de Coleta.csv"
     * 2.2 Para cada questão:
     *     - Enriquecer query com termos do guia
     *     - Buscar chunks relevantes via RAG
     *     - Construir prompt estruturado
     *     - Enviar para Google Gemini
     *     - Pós-processar resposta
     * 2.3 Salva respostas em output/respostas.csv
     * 
     * @param args Argumentos de linha de comando (opcional: substring do PDF a processar)
     */
    public static void main(String[] args) {
        // =====================
        // FASE 1: INDEXAÇÃO
        // =====================
        printBanner(); // Passo 1.1: Exibe banner
        Config.printConfig(); // Passo 1.2: Carrega configurações

        try {
            // Passo 1.3: Listar todos os PDFs na pasta data/report
            File folder = new File(Config.DATA_FOLDER);
            File[] pdfFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
            if (pdfFiles == null || pdfFiles.length == 0) {
                System.err.println("❌ Nenhum PDF encontrado em " + Config.DATA_FOLDER);
                return;
            }

            // Permitir rodar apenas um PDF específico via argumento
            String pdfToProcess = null;
            if (args.length > 0) {
                pdfToProcess = args[0];
            }

            // =====================
            // FASE 2: PREPARAÇÃO DO OUTPUT CSV
            // =====================
            // Passo 2.3: Salvar respostas em output/respostas.csv
            Path outputPath = Path.of("output/respostas.csv");
            Files.createDirectories(outputPath.getParent());
            PrintWriter writer = new PrintWriter(Files.newBufferedWriter(outputPath));
            // Cabeçalho do CSV de respostas (CompanyResponse.csvHeader)
            writer.println(CompanyResponse.csvHeader());

            for (File pdfFile : pdfFiles) {
                if (pdfToProcess != null && !pdfFile.getName().toLowerCase().contains(pdfToProcess.trim().toLowerCase())) {
                    System.out.println("[INFO] Ignorando arquivo: " + pdfFile.getName() + " (esperado substring: " + pdfToProcess + ")");
                    continue;
                }
                System.out.println("[INFO] Processando arquivo: " + pdfFile.getName());
                System.out.println("\n" + "=".repeat(80));
                System.out.println("INDEXANDO: " + pdfFile.getName());
                System.out.println("=".repeat(80));

                // Passo 1.4: Indexação do PDF
                // - Extrai texto, divide em chunks, gera embeddings, armazena em memória
                DocumentIndexer indexer = new DocumentIndexer();
                indexer.indexDocument(pdfFile.getAbsolutePath());

                // Instancia motor de busca RAG para o PDF indexado
                RagQueryEngine queryEngine = new RagQueryEngine(
                        indexer.getEmbeddingStore(),
                        indexer.getEmbeddingModel()
                );

                // Nome da empresa = nome do arquivo (sem .pdf)
                String companyName = pdfFile.getName().replaceFirst("\\.pdf$", "");
                QuestionProcessor processor = new QuestionProcessor(queryEngine);
                CsvQuestionReader reader = new CsvQuestionReader();
                CompanyResponse response = new CompanyResponse(companyName);

                // =====================
                // FASE 2: PROCESSAMENTO DE QUESTÕES
                // =====================
                // Passo 2.1: Ler questões do arquivo "Guia de Coleta.csv"
                // CsvQuestionReader lê o arquivo do guia de coleta
                int numQuestionsToProcess = 5; // Limitar às 24 primeiras questões
                for (int index = 0; index < numQuestionsToProcess; index++) {
                    // Passo 2.2: Para cada questão
                    Question question = reader.readQuestionByIndex(index);
                    if (question == null) continue;
                    // - Enriquecer query com termos do guia
                    // - Buscar chunks relevantes via RAG
                    // - Construir prompt estruturado
                    // - Enviar para Google Gemini
                    // - Pós-processar resposta
                    String answer = processor.processQuestion(question);
                    response.setResposta(question.getNumero(), answer);
                    // Delay entre requisições para respeitar o rate limit
                    if (index < numQuestionsToProcess - 1) {
                        try {
                            System.out.println("⏳ Aguardando " + (Config.REQUEST_DELAY_MS/1000.0) + "s antes da próxima questão (rate limiting)...");
                            Thread.sleep(Config.REQUEST_DELAY_MS);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }

                // Passo extra: Preencher respostas manuais se existir linha correspondente no CSV manual
                // (output/respostas-analise_manual.csv)
                Path manualPath = Path.of("output/respostas-analise_manual.csv");
                if (Files.exists(manualPath)) {
                    List<String> manualLines = Files.readAllLines(manualPath);
                    for (String line : manualLines) {
                        String[] fields = line.split(";");
                        if (fields.length > 0 && fields[0].trim().equalsIgnoreCase(companyName.trim())) {
                            response.preencherRespostasManuais(fields);
                            break;
                        }
                    }
                }

                // Passo 2.3: Salvar linha de respostas da empresa no CSV de output
                writer.println(response.toCsvLine());
                writer.flush();
            }
            writer.close();
        } catch (Exception e) {
            System.err.println("\n❌ Erro na execução: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("\n✅ Aplicação finalizada!");
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
        System.out.println("║  • AllMiniLmL6V2 - Embeddings locais (384 dim)                                ║");
        System.out.println("║  • Apache Tika - Extração de texto de PDFs                                    ║");
        System.out.println("║                                                                               ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════════════════╝");
    }
}
