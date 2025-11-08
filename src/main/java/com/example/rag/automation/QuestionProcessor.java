package com.example.rag.automation;

import com.example.rag.automation.model.Question;
import com.example.rag.retrieval.RagQueryEngine;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Processador de questões usando RAG.
 * 
 * Estratégia:
 * 1. Enriquece query de busca com termos do guia
 * 2. Busca chunks relevantes via RAG
 * 3. Constrói prompt estruturado com orientações
 * 4. Envia para Gemini
 * 5. Pós-processa resposta aplicando regras
 */
public class QuestionProcessor {
    
    private final RagQueryEngine ragEngine;
    
    public QuestionProcessor(RagQueryEngine ragEngine) {
        this.ragEngine = ragEngine;
    }
    
    /**
     * Processa uma questão e retorna a resposta.
     * 
     * @param question Questão do guia
     * @return Resposta formatada
     */
    public String processQuestion(Question question) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📋 Processando Questão " + question.getNumero() + ": " + question.getQuestao());
        System.out.println("=".repeat(80));
        
        try {
            // PASSO 1: Enriquecer query de busca
            String enrichedQuery = buildEnrichedSearchQuery(question);
            System.out.println("\n🔍 Query enriquecida:");
            System.out.println("   " + enrichedQuery.substring(0, Math.min(100, enrichedQuery.length())) + "...");
            
            // PASSO 2: Buscar chunks relevantes
            System.out.println("\n🔎 Buscando chunks relevantes...");
            List<EmbeddingMatch<TextSegment>> matches = ragEngine.retrieveOnly(enrichedQuery);
            
            if (matches.isEmpty()) {
                System.out.println("   ⚠️ Nenhum chunk relevante encontrado!");
                return "INFORMAÇÃO NÃO ENCONTRADA";
            }
            
            System.out.println("   ✅ Encontrados " + matches.size() + " chunks");
            printTopMatches(matches, 3);
            
            // PASSO 3: Construir contexto
            String context = buildContext(matches);
            
            // PASSO 4: Criar prompt estruturado
            String structuredPrompt = buildStructuredPrompt(question, context);
            
            // PASSO 5: Enviar para Gemini
            System.out.println("\n🤖 Enviando para Gemini...");
            String rawAnswer = ragEngine.query(structuredPrompt);
            
            // PASSO 6: Pós-processar resposta
            String finalAnswer = postProcessAnswer(rawAnswer, question);
            
            System.out.println("\n✅ Resposta final: " + finalAnswer);
            return finalAnswer;
            
        } catch (Exception e) {
            System.err.println("❌ Erro ao processar questão: " + e.getMessage());
            e.printStackTrace();
            return "ERRO: " + e.getMessage();
        }
    }
    
    /**
     * Enriquece query de busca com termos-chave do guia.
     * 
     * ESTRATÉGIA OTIMIZADA:
     * 1. Questão original (peso alto)
     * 2. Localização do documento (ex: "2.1.h", "FR") 
     * 3. Termos-chave de "Como Preencher" (aspas + termos importantes)
     * 4. Termos-chave de "Observações" (aspas + contexto adicional)
     * 5. Expansão semântica (sinônimos para melhorar busca)
     */
    private String buildEnrichedSearchQuery(Question q) {
        StringBuilder query = new StringBuilder();
        
        // 1. Questão original (sempre primeiro)
        query.append(q.getQuestao()).append(" ");
        
        // 2. Localização no documento (CRÍTICO para documentos estruturados)
        if (q.getOnde() != null && !q.getOnde().isEmpty()) {
            query.append(q.getOnde()).append(" ");
            
            // Adicionar variações da localização
            String onde = q.getOnde();
            if (onde.contains("2.1")) {
                query.append("Condições financeiras patrimoniais ");
            }
            if (onde.contains("FR")) {
                query.append("Formulário Referência ");
            }
        }
        
        // 3. Termos entre aspas de "Como Preencher" (termos exatos)
        if (q.getComoPreencher() != null) {
            List<String> keywords = extractKeywords(q.getComoPreencher());
            keywords.forEach(k -> query.append(k).append(" "));
            
            // Adicionar também termos importantes SEM aspas
            String comoPreencher = q.getComoPreencher();
            if (comoPreencher.contains("Receita")) {
                query.append("Receita líquida operacional demonstração resultado ");
            }
            if (comoPreencher.contains("Lucro")) {
                query.append("Lucro líquido resultado exercício prejuízo tabela ");
            }
            if (comoPreencher.contains("auditoria") || comoPreencher.contains("Auditor")) {
                query.append("auditoria independente auditor responsável firma ");
            }
            if (comoPreencher.contains("honorários") || comoPreencher.contains("gastos")) {
                query.append("honorários remuneração valores pagos custos ");
            }
            if (comoPreencher.contains("mil") || comoPreencher.contains("milhão")) {
                query.append("R$ mil milhão valores monetários tabela ");
            }
        }
        
        // 4. Termos entre aspas de "Observações"
        if (q.getObservacoes() != null) {
            List<String> obsKeywords = extractKeywords(q.getObservacoes());
            obsKeywords.forEach(k -> query.append(k).append(" "));
            
            // Contexto adicional de observações
            String obs = q.getObservacoes();
            if (obs.toLowerCase().contains("banco")) {
                query.append("banco instituição financeira ");
            }
            if (obs.toLowerCase().contains("df")) {
                query.append("demonstrações financeiras balanço ");
            }
        }
        
        return query.toString().trim();
    }
    
    /**
     * Extrai palavras-chave importantes (texto entre aspas).
     */
    private List<String> extractKeywords(String text) {
        List<String> keywords = new ArrayList<>();
        
        // Extrair texto entre aspas duplas
        Pattern pattern = Pattern.compile("\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(text);
        
        while (matcher.find()) {
            String keyword = matcher.group(1);
            if (!keyword.isEmpty()) {
                keywords.add(keyword);
            }
        }
        
        return keywords;
    }
    
    /**
     * Constrói contexto a partir dos chunks recuperados.
     */
    private String buildContext(List<EmbeddingMatch<TextSegment>> matches) {
        return matches.stream()
                .map(match -> match.embedded().text())
                .collect(Collectors.joining("\n\n---\n\n"));
    }
    
    /**
     * Constrói prompt estruturado com orientações do guia.
     */
    private String buildStructuredPrompt(Question q, String context) {
        return String.format("""
            Você é um assistente especializado em análise de Formulários de Referência (FR).
            
            TAREFA: Extrair informação EXATA do documento fornecido.
            
            QUESTÃO: %s
            
            LOCALIZAÇÃO NO DOCUMENTO: %s
            
            INSTRUÇÕES ESPECÍFICAS:
            %s
            
            OBSERVAÇÕES IMPORTANTES:
            %s
            
            DOCUMENTOS RELEVANTES:
            %s
            
            REGRAS IMPORTANTES:
            - Busque EXATAMENTE os termos mencionados nas instruções
            - Se houver múltiplas possibilidades (ex: banco vs empresa normal), identifique qual se aplica
            - Se o valor estiver em "R$ mil" ou "R$ milhão", SEMPRE informe a unidade na resposta
            - Retorne APENAS o valor/informação solicitada, sem explicações adicionais
            - Se não encontrar a informação, responda: "INFORMAÇÃO NÃO ENCONTRADA"
            - Para valores monetários, use o formato: [número] (em R$ mil) ou [número] (em R$ milhão)
            
            RESPOSTA (apenas o valor no formato especificado):
            """,
            q.getQuestao(),
            q.getOnde() != null ? q.getOnde() : "Não especificado",
            q.getComoPreencher() != null ? q.getComoPreencher() : "Não especificado",
            q.getObservacoes() != null ? q.getObservacoes() : "Nenhuma",
            context
        );
    }
    
    /**
     * Pós-processa resposta aplicando regras do guia.
     */
    private String postProcessAnswer(String rawAnswer, Question q) {
        String processed = rawAnswer.trim();
        
        // Para questões monetárias (2, 3, 6, 8): aplicar regras de multiplicação
        if (q.getNumero() == 2 || q.getNumero() == 3 || q.getNumero() == 6 || q.getNumero() == 8) {
            processed = applyMonetaryRules(processed);
        }
        
        // Para questão 5 (firma de auditoria): limpeza de texto
        if (q.getNumero() == 5) {
            processed = cleanAuditResponse(processed);
        }
        
        return processed;
    }
    
    /**
     * Limpa respostas relacionadas a auditoria.
     * Remove textos explicativos desnecessários.
     */
    private String cleanAuditResponse(String response) {
        // Se a resposta for muito longa (>200 chars), é provável que tenha texto extra
        if (response.length() > 200) {
            // Tenta extrair apenas o essencial (primeira linha ou primeira frase)
            String[] lines = response.split("\n");
            if (lines.length > 0 && lines[0].length() < 150) {
                return lines[0].trim();
            }
        }
        return response;
    }
    
    /**
     * Aplica regras monetárias (multiplicação por mil/milhão).
     * 
     * Detecta padrões como:
     * - "1.234.567 (em R$ mil)"
     * - "1.234 (em milhão)"
     * - "R$ 1.234.567 mil"
     */
    private String applyMonetaryRules(String value) {
        // Padrão para capturar: número + unidade (mil/milhão)
        Pattern pattern = Pattern.compile("([\\d.,]+)\\s*(?:\\()?(?:em)?\\s*R?\\$?\\s*(mil|milhão|milhões|thousand|million)?(?:\\))?", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(value);
        
        if (matcher.find()) {
            String numberStr = matcher.group(1);
            String unit = matcher.group(2);
            
            try {
                // Remover pontos de milhar e trocar vírgula por ponto
                String cleanNumber = numberStr.replace(".", "").replace(",", ".");
                double number = Double.parseDouble(cleanNumber);
                
                // Aplicar multiplicação conforme unidade
                if (unit != null) {
                    if (unit.toLowerCase().contains("mil") || unit.equalsIgnoreCase("thousand")) {
                        number *= 1000;
                    } else if (unit.toLowerCase().contains("milhão") || unit.toLowerCase().contains("milhões") || unit.equalsIgnoreCase("million")) {
                        number *= 1000000;
                    }
                }
                
                // Formatar como moeda brasileira
                return formatCurrency(Math.round(number));
                
            } catch (NumberFormatException e) {
                System.err.println("⚠️ Erro ao converter número: " + numberStr);
                return value;
            }
        }
        
        return value;
    }
    
    /**
     * Formata valor como moeda brasileira.
     * 
     * @param value Valor numérico
     * @return String formatada (ex: R$ 1.234.567.000)
     */
    private String formatCurrency(long value) {
        // Formatar com separadores de milhar (ponto)
        String formatted = String.format("%,d", value).replace(',', '.');
        return "R$ " + formatted;
    }
    
    /**
     * Imprime os top matches para debug.
     */
    private void printTopMatches(List<EmbeddingMatch<TextSegment>> matches, int top) {
        System.out.println("\n   📄 Top " + top + " chunks mais relevantes:");
        
        int count = Math.min(top, matches.size());
        for (int i = 0; i < count; i++) {
            EmbeddingMatch<TextSegment> match = matches.get(i);
            String preview = match.embedded().text()
                    .substring(0, Math.min(120, match.embedded().text().length()))
                    .replace("\n", " ");
            
            System.out.printf("      [%d] Score: %.4f | %s...%n", 
                    i + 1, match.score(), preview);
        }
    }
}
