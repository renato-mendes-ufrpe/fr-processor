package com.example.rag.automation;

import com.example.rag.automation.model.Question;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Leitor do arquivo "Guia de Coleta.csv".
 * 
 * Trata CSV com:
 * - Separador: ponto e vírgula (;)
 * - Aspas duplas escapadas ("")
 * - Quebras de linha dentro de células
 */
public class CsvQuestionReader {
    
    private static final String CSV_FILE_PATH = "Guia de Coleta.csv";
    
    /**
     * Lê uma questão específica do CSV pelo número.
     * 
     * @param numero Número da questão (coluna "Nº")
     * @return Question com os dados ou null se não encontrar
     * @throws IOException Se houver erro ao ler o arquivo
     */
    public Question readQuestion(int numero) throws IOException {
        List<Question> allQuestions = readAllQuestions();
        
        return allQuestions.stream()
                .filter(q -> q.getNumero() == numero)
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Lê uma questão específica pela posição (índice) na lista.
     * 
     * @param index Índice da questão (0-based)
     * @return Question com os dados ou null se índice inválido
     * @throws IOException Se houver erro ao ler o arquivo
     */
    public Question readQuestionByIndex(int index) throws IOException {
        List<Question> allQuestions = readAllQuestions();
        
        if (index >= 0 && index < allQuestions.size()) {
            return allQuestions.get(index);
        }
        
        return null;
    }
    
    /**
     * Lê todas as questões do CSV.
     * 
     * @return Lista de questões
     * @throws IOException Se houver erro ao ler o arquivo
     */
    public List<Question> readAllQuestions() throws IOException {
        List<Question> questions = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(CSV_FILE_PATH))) {
            // Pular cabeçalho
            String headerLine = br.readLine();
            if (headerLine == null) {
                throw new IOException("Arquivo CSV vazio");
            }
            
            String line;
            StringBuilder currentLine = new StringBuilder();
            boolean insideQuotes = false;
            
            while ((line = br.readLine()) != null) {
                currentLine.append(line);
                
                // Contar aspas para detectar quebras de linha dentro de células
                long quoteCount = line.chars().filter(ch -> ch == '"').count();
                
                // Se número ímpar de aspas, estamos dentro de uma célula com quebra de linha
                if (quoteCount % 2 != 0) {
                    insideQuotes = !insideQuotes;
                }
                
                // Se não estamos dentro de aspas, processar a linha completa
                if (!insideQuotes) {
                    Question question = parseLine(currentLine.toString());
                    if (question != null) {
                        questions.add(question);
                    }
                    currentLine = new StringBuilder();
                }  else {
                    // Adicionar quebra de linha para preservar formatação
                    currentLine.append("\n");
                }
            }
        }
        
        System.out.println("✅ Carregadas " + questions.size() + " questões do guia");
        return questions;
    }
    
    /**
     * Faz parse de uma linha do CSV.
     * 
     * Formato: Nº;Dificuldade;Questão;Onde?;Como Preencher?;OBSERVAÇÕES;Tipo
     * 
     * @param line Linha completa do CSV
     * @return Question ou null se houver erro
     */
    private Question parseLine(String line) {
        try {
            List<String> fields = splitCsvLine(line);
            
            if (fields.size() < 7) {
                System.err.println("⚠️ Linha com campos insuficientes: " + line.substring(0, Math.min(50, line.length())));
                return null;
            }
            
            Question question = new Question();
            question.setNumero(parseIntSafe(fields.get(0)));
            question.setDificuldade(cleanField(fields.get(1)));
            question.setQuestao(cleanField(fields.get(2)));
            question.setOnde(cleanField(fields.get(3)));
            question.setComoPreencher(cleanField(fields.get(4)));
            question.setObservacoes(cleanField(fields.get(5)));
            question.setTipo(parseTipoQuestao(cleanField(fields.get(6))));
            
            return question;
            
        } catch (Exception e) {
            System.err.println("❌ Erro ao processar linha: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Split de linha CSV respeitando aspas e ponto e vírgula.
     * 
     * @param line Linha do CSV
     * @return Lista de campos
     */
    private List<String> splitCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean insideQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                // Checar se é aspas duplas escapadas ("")
                if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    currentField.append('"');
                    i++; // Pular próxima aspas
                } else {
                    insideQuotes = !insideQuotes;
                }
            } else if (c == ';' && !insideQuotes) {
                fields.add(currentField.toString());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }
        
        // Adicionar último campo
        fields.add(currentField.toString());
        
        return fields;
    }
    
    /**
     * Limpa campo CSV (remove aspas extras, trim).
     * 
     * @param field Campo bruto
     * @return Campo limpo
     */
    private String cleanField(String field) {
        if (field == null) {
            return "";
        }
        
        String cleaned = field.trim();
        
        // Remover aspas do início e fim se existirem
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        
        // Substituir aspas duplas escapadas por aspas simples
        cleaned = cleaned.replace("\"\"", "\"");
        
        return cleaned.trim();
    }
    
    /**
     * Parse seguro de int.
     * 
     * @param value String com número
     * @return int ou 0 se inválido
     */
    private int parseIntSafe(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * Parse do tipo de questão.
     * 
     * @param value String com o tipo (MONETARIA, SIM_NAO, etc)
     * @return TipoQuestao ou TEXTO_ESPECIFICO como padrão
     */
    private com.example.rag.automation.model.TipoQuestao parseTipoQuestao(String value) {
        if (value == null || value.isEmpty()) {
            return com.example.rag.automation.model.TipoQuestao.TEXTO_ESPECIFICO;
        }
        
        try {
            return com.example.rag.automation.model.TipoQuestao.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("⚠️ Tipo de questão inválido: " + value + ". Usando TEXTO_ESPECIFICO como padrão.");
            return com.example.rag.automation.model.TipoQuestao.TEXTO_ESPECIFICO;
        }
    }
}
