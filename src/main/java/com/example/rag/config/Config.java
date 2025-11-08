package com.example.rag.config;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Classe de configuração centralizada para o projeto RAG (Retrieval-Augmented Generation).
 * 
 * Esta classe gerencia todas as configurações do sistema:
 * - Configurações do Google Gemini (API Key e modelo)
 * - Parâmetros de chunking (divisão de documentos)
 * - Parâmetros de retrieval (busca por similaridade)
 * - Caminhos de arquivos e diretórios
 * 
 * Funcionamento:
 * 1. Carrega automaticamente o arquivo .env (se existir)
 * 2. Configura variáveis de ambiente do sistema
 * 3. Valida se o Gemini está configurado
 * 
 * Nota: O modelo de embeddings (AllMiniLmL6V2) é local e gratuito, não requer configuração.
 * Apenas o Gemini (geração de respostas) é opcional.
 */
public class Config {
    
    // ========================================
    // CONFIGURAÇÕES DO GOOGLE GEMINI (Opcional)
    // ========================================
    
    /**
     * Chave da API do Google Gemini.
     * Obtida de: https://aistudio.google.com/app/apikey
     * Carregada do arquivo .env ou variável de ambiente GEMINI_API_KEY
     */
    public static final String GEMINI_API_KEY;
    
    /**
     * Nome do modelo Gemini a ser usado.
     * 
     * Modelos disponíveis:
     * - gemini-2.5-flash: Mais rápido e barato (recomendado)
     * - gemini-2.5-pro: Mais poderoso, mais caro
     * - gemini-1.5-flash: Versão anterior
     * 
     * Padrão: gemini-2.5-flash
     */
    public static final String GEMINI_MODEL;
    
    /**
     * Delay entre requisições para a API do Gemini (em milissegundos).
     * 
     * Limites do Free Tier (https://ai.google.dev/gemini-api/docs/rate-limits):
     * - Gemini 2.5 Flash: 10 RPM (Requests Per Minute)
     * - Gemini 2.5 Pro: 2 RPM
     * 
     * Valores recomendados:
     * - Free Tier Flash: 6000ms (6s) = 10 requests/min com margem de segurança
     * - Free Tier Pro: 30000ms (30s) = 2 requests/min
     * - Nível 1 (pago): 100ms ou menos
     * 
     * Padrão: 6000ms (adequado para Free Tier do Flash)
     */
    public static final long REQUEST_DELAY_MS;
    
    /**
     * Intervalo para salvar checkpoint do processamento.
     * 
     * A cada N questões processadas, o sistema salva o progresso em arquivo CSV.
     * Útil para retomar o processamento em caso de erro ou interrupção.
     * 
     * Padrão: 5 questões
     */
    public static final int CHECKPOINT_INTERVAL;
    
    // Bloco estático que executa ao carregar a classe
    static {
        // Primeiro, tenta carregar o arquivo .env
        loadEnv();
        
        // Depois, lê as variáveis (prioriza System.getProperty, depois System.getenv)
        GEMINI_API_KEY = System.getProperty("GEMINI_API_KEY", 
                        System.getenv().getOrDefault("GEMINI_API_KEY", ""));
        GEMINI_MODEL = System.getProperty("GEMINI_MODEL",
                      System.getenv().getOrDefault("GEMINI_MODEL", "gemini-2.5-flash"));
        
        // Rate limiting configurations
        String delayStr = System.getProperty("REQUEST_DELAY_MS",
                         System.getenv().getOrDefault("REQUEST_DELAY_MS", "6000"));
        REQUEST_DELAY_MS = Long.parseLong(delayStr);
        
        String checkpointStr = System.getProperty("CHECKPOINT_INTERVAL",
                              System.getenv().getOrDefault("CHECKPOINT_INTERVAL", "5"));
        CHECKPOINT_INTERVAL = Integer.parseInt(checkpointStr);
        
        // RAG configurations (chunking and retrieval)
        String segmentSizeStr = System.getProperty("MAX_SEGMENT_SIZE_IN_TOKENS",
                               System.getenv().getOrDefault("MAX_SEGMENT_SIZE_IN_TOKENS", "1200"));
        MAX_SEGMENT_SIZE_IN_TOKENS = Integer.parseInt(segmentSizeStr);
        
        String overlapStr = System.getProperty("SEGMENT_OVERLAP_IN_TOKENS",
                           System.getenv().getOrDefault("SEGMENT_OVERLAP_IN_TOKENS", "200"));
        SEGMENT_OVERLAP_IN_TOKENS = Integer.parseInt(overlapStr);
        
        String maxResultsStr = System.getProperty("MAX_RESULTS_FOR_RETRIEVAL",
                              System.getenv().getOrDefault("MAX_RESULTS_FOR_RETRIEVAL", "15"));
        MAX_RESULTS_FOR_RETRIEVAL = Integer.parseInt(maxResultsStr);
        
        String minScoreStr = System.getProperty("MIN_SCORE_FOR_RETRIEVAL",
                            System.getenv().getOrDefault("MIN_SCORE_FOR_RETRIEVAL", "0.60"));
        MIN_SCORE_FOR_RETRIEVAL = Double.parseDouble(minScoreStr);
    }
    
    // ========================================
    // CONFIGURAÇÕES DE RAG (Chunking e Retrieval)
    // ========================================
    
    /**
     * Tamanho máximo de cada chunk (segmento) em tokens.
     * 
     * Chunks menores = mais precisão, mas mais processamento
     * Chunks maiores = mais contexto, mas menos precisão
     * 
     * Para documentos financeiros complexos (com tabelas, valores, seções):
     * - Recomendado: 1000-1500 tokens (permite capturar tabelas completas)
     * 
     * Padrão: 1200 tokens (~900 palavras, ~6 parágrafos)
     * Configurável via .env: MAX_SEGMENT_SIZE_IN_TOKENS
     */
    public static final int MAX_SEGMENT_SIZE_IN_TOKENS;
    
    /**
     * Quantidade de tokens que se sobrepõem entre chunks consecutivos.
     * 
     * Overlap ajuda a não perder contexto nas divisões.
     * Recomendado: 10-20% do tamanho do chunk
     * 
     * Padrão: 200 tokens (~16% de 1200)
     * Configurável via .env: SEGMENT_OVERLAP_IN_TOKENS
     */
    public static final int SEGMENT_OVERLAP_IN_TOKENS;
    
    /**
     * Quantidade máxima de chunks a serem recuperados na busca por similaridade.
     * 
     * Mais resultados = mais contexto, mas prompt maior e mais caro
     * 
     * Padrão: 15 resultados
     * Configurável via .env: MAX_RESULTS_FOR_RETRIEVAL
     */
    public static final int MAX_RESULTS_FOR_RETRIEVAL;
    
    /**
     * Score mínimo de similaridade para considerar um chunk relevante.
     * 
     * Valor entre 0.0 (nenhuma similaridade) e 1.0 (idêntico)
     * Score muito alto = pode não encontrar nada
     * Score muito baixo = pode trazer contexto irrelevante
     * 
     * Padrão: 0.60
     * Configurável via .env: MIN_SCORE_FOR_RETRIEVAL
     */
    public static final double MIN_SCORE_FOR_RETRIEVAL;
    
    // ========================================
    // CAMINHOS DE ARQUIVOS
    // ========================================
    
    /**
     * Pasta onde os PDFs estão armazenados.
     * Caminho relativo à raiz do projeto.
     */
    public static final String DATA_FOLDER = "data/report";
    
    /**
     * Nome do arquivo PDF a ser indexado.
     * Deve estar localizado dentro de DATA_FOLDER.
     * 
     * IMPORTANTE: Altere este valor para usar seus próprios documentos!
     */
    public static final String AMBIPAR_PDF_FILE = "AMBIPAR PARTICIPAÇÕES E EMPREENDIMENTOS S.A..pdf";
    
    // ========================================
    // MÉTODOS UTILITÁRIOS
    // ========================================
    
    /**
     * Verifica se o Google Gemini está configurado e pronto para uso.
     * 
     * O Gemini é considerado configurado quando:
     * - A variável GEMINI_API_KEY não está vazia
     * - A chave não é o valor placeholder "sua-chave-aqui"
     * 
     * Se não estiver configurado, o sistema funciona em modo "somente retrieval",
     * retornando apenas o contexto encontrado, sem gerar respostas.
     * 
     * @return true se o Gemini está configurado, false caso contrário
     */
    public static boolean isGeminiConfigured() {
        return GEMINI_API_KEY != null && 
               !GEMINI_API_KEY.isEmpty() && 
               !GEMINI_API_KEY.equals("sua-chave-aqui");
    }
    
    /**
     * Exibe no console todas as configurações atuais do sistema.
     * 
     * Mostra:
     * - Modelo de embeddings (sempre local)
     * - Parâmetros de chunking
     * - Parâmetros de retrieval
     * - Status do Gemini (configurado ou não)
     * 
     * Útil para debug e validação da configuração ao iniciar a aplicação.
     */
    public static void printConfig() {
        System.out.println("📋 Configurações:");
        System.out.println("   Embedding Model: AllMiniLmL6V2 (local, offline)");
        System.out.println("   Max Segment Size: " + MAX_SEGMENT_SIZE_IN_TOKENS + " tokens");
        System.out.println("   Segment Overlap: " + SEGMENT_OVERLAP_IN_TOKENS + " tokens");
        System.out.println("   Max Results: " + MAX_RESULTS_FOR_RETRIEVAL);
        System.out.println("   Min Score: " + MIN_SCORE_FOR_RETRIEVAL);
        
        if (isGeminiConfigured()) {
            System.out.println("   Gemini: ✅ Configurado (" + GEMINI_MODEL + ")");
            System.out.println("   Rate Limiting:");
            System.out.println("      • Delay entre requests: " + REQUEST_DELAY_MS + "ms (" + (REQUEST_DELAY_MS/1000.0) + "s)");
            System.out.println("      • Checkpoint a cada: " + CHECKPOINT_INTERVAL + " questões");
            
            // Calcular e mostrar taxa máxima
            double maxRequestsPerMinute = 60000.0 / REQUEST_DELAY_MS;
            System.out.println("      • Taxa máxima: ~" + String.format("%.1f", maxRequestsPerMinute) + " requests/min");
        } else {
            System.out.println("   Gemini: ⚠️  Não configurado (apenas retrieval)");
            System.out.println("   💡 Para habilitar Gemini: configure GEMINI_API_KEY no arquivo .env");
        }
    }
    
    /**
     * Carrega variáveis de ambiente do arquivo .env na raiz do projeto.
     * 
     * Formato do arquivo .env:
     * GEMINI_API_KEY=AIza...
     * GEMINI_MODEL=gemini-2.5-flash
     * 
     * Como funciona:
     * 1. Verifica se existe um arquivo .env na raiz do projeto
     * 2. Lê linha por linha
     * 3. Ignora linhas vazias e comentários (começam com #)
     * 4. Para cada linha "CHAVE=VALOR", define System.setProperty(CHAVE, VALOR)
     * 
     * Nota: Este método é chamado automaticamente no bloco static da classe,
     * antes de qualquer uso das configurações.
     */
    private static void loadEnv() {
        Path envPath = Paths.get(".env");
        
        // Verifica se o arquivo .env existe
        if (Files.exists(envPath)) {
            try (BufferedReader reader = new BufferedReader(new FileReader(envPath.toFile()))) {
                String line;
                
                // Lê cada linha do arquivo
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    
                    // Ignora linhas vazias ou comentários
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    
                    // Divide a linha em "chave=valor"
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        String key = parts[0].trim();
                        String value = parts[1].trim();
                        
                        // Define como propriedade do sistema
                        System.setProperty(key, value);
                    }
                }
                System.out.println("✅ Arquivo .env carregado");
            } catch (Exception e) {
                System.out.println("⚠️  Erro ao carregar .env: " + e.getMessage());
            }
        } else {
            System.out.println("ℹ️  Arquivo .env não encontrado (opcional)");
        }
    }
}
