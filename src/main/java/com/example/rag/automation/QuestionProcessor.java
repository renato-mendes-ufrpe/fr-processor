package com.example.rag.automation;

import com.example.rag.automation.model.Question;
import com.example.rag.retrieval.RagQueryEngine;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;

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
            // Se rawAnswer for nulo (ex: erro de API/rate-limit), tratar como não encontrada
            if (rawAnswer == null) {
                System.err.println("   ⚠️ Resposta do LLM é nula — tratando como informação não encontrada (provável erro na chamada ao modelo)");
                return "INFORMAÇÃO NÃO ENCONTRADA";
            }

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
        }
        
        // 3. Palavras-chave RAG (nova coluna)
        if (q.getPalavrasChaveRag() != null && !q.getPalavrasChaveRag().isEmpty()) {
            query.append(q.getPalavrasChaveRag()).append(" ");
        }
        
        return query.toString().trim();
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
     * 
     * Estratégia: Prompts especializados por tipo para melhor acurácia.
     */
    private String buildStructuredPrompt(Question q, String context) {
        // Usar prompt especializado conforme o tipo
        switch (q.getTipo()) {
            case MONETARIA:
                return buildMonetaryPrompt(q, context);
            case SIM_NAO:
                return buildYesNoPrompt(q, context);
            case CONTAGEM:
                return buildCountingPrompt(q, context);
            case TEXTO_ESPECIFICO:
                return buildTextPrompt(q, context);
            case MULTIPLA_ESCOLHA:
                return buildMultipleChoicePrompt(q, context);
            default:
                return buildGenericPrompt(q, context);
        }
    }
    
    /**
     * Prompt especializado para questões monetárias.
     */
    private String buildMonetaryPrompt(Question q, String context) {
        return String.format("""
            Você é um assistente especializado em extrair valores monetários de Formulários de Referência.
            
            TAREFA: Extrair o valor monetário EXATO da seção indicada.
            
            QUESTÃO: %s
            
            LOCALIZAÇÃO: %s
            
            INSTRUÇÕES:
            %s
            
            DOCUMENTOS:
            %s
            
            REGRAS CRÍTICAS:
            1. Retorne APENAS o número com unidade (ex: "4.872.707 (em R$ mil)" ou "56.649 (em milhão)")
            2. SEMPRE identifique se o valor está em R$ mil, R$ milhão ou valor absoluto
            3. Busque em tabelas da seção indicada (geralmente 2.1.h ou demonstrações financeiras)
            4. Para bancos: "Receitas da Intermediação Financeira" = Receita Líquida
            5. Para prejuízo: inclua o sinal negativo (-)
            6. Se não encontrar: "INFORMAÇÃO NÃO ENCONTRADA"
            7. NÃO inclua explicações, textos adicionais ou fórmulas
            
            RESPOSTA (apenas número + unidade):
            """,
            q.getQuestao(),
            q.getOnde() != null ? q.getOnde() : "FR",
            q.getComoPreencher() != null ? q.getComoPreencher() : "",
            context
        );
    }
    
    /**
     * Prompt especializado para questões SIM/NÃO.
     */
    private String buildYesNoPrompt(Question q, String context) {
        return String.format("""
            Você é um assistente especializado em análise de Formulários de Referência.
            
            TAREFA: Responder SIM, NÃO, NÃO DIVULGADO ou NÃO APLICADO com base no documento.
            
            QUESTÃO: %s
            
            LOCALIZAÇÃO: %s
            
            CRITÉRIOS DE DECISÃO:
            %s
            
            DOCUMENTOS:
            %s
            
            REGRAS CRÍTICAS:
            1. Retorne APENAS uma das opções: "SIM", "NÃO", "NÃO DIVULGADO" ou "NÃO APLICADO"
            2. NÃO inclua "=" ou texto explicativo (ex: ERRADO: "SIM = a empresa cita...")
            3. NÃO inclua ponto final ou qualquer pontuação
            4. SIM: quando o documento AFIRMA explicitamente
            5. NÃO: quando o documento NEGA explicitamente
            6. NÃO DIVULGADO: quando não há informação no documento
            7. NÃO APLICADO: quando não se aplica ao caso
            
            RESPOSTA (apenas SIM, NÃO, NÃO DIVULGADO ou NÃO APLICADO):
            """,
            q.getQuestao(),
            q.getOnde() != null ? q.getOnde() : "FR",
            q.getComoPreencher() != null ? q.getComoPreencher() : "",
            context
        );
    }
    
    /**
     * Prompt especializado para questões de contagem.
     */
    private String buildCountingPrompt(Question q, String context) {
        return String.format("""
            TAREFA: %s
            
            DOCUMENTOS:
            %s
            
            ═══════════════════════════════════════════════════════════════
            REGRA ABSOLUTA - Identificar membros corretamente:
            ═══════════════════════════════════════════════════════════════
            
            CONSELHEIRO = SOMENTE se tiver esta estrutura:
               Nome: [NOME COMPLETO]
               CPF: [###.###.###-##]
               Órgãos da Administração:
                  Órgão da Administração: "Conselho de Administração"
            
            NÃO É CONSELHEIRO se:
               • Órgão da Administração = "Diretoria" (mesmo que seja diretor)
               • Só aparece em seção "Comitês:" (sem tabela "Órgãos da Administração")
               • Não tem a coluna "Órgão da Administração" = "Conselho de Administração"
            
            ═══════════════════════════════════════════════════════════════
            TIPOS DE CONSELHEIROS (veja coluna "Cargo eletivo ocupado"):
            ═══════════════════════════════════════════════════════════════
            
            INDEPENDENTE:
               "Cargo eletivo ocupado" contém "Independente"
               Exemplos: "Conselho de Adm. Independente (Efetivo)"
               DEVE ter "Órgão da Administração" = "Conselho de Administração"

            EXTERNO:
               "Cargo eletivo ocupado" = "Conselho de Administração (Efetivo)"
               SEM palavra "Independente" E SEM palavra "Diretor"
               DEVE ter "Órgão da Administração" = "Conselho de Administração"

            EXECUTIVO:
               Aparece em DUAS linhas: uma com Diretoria E outra com Conselho
               OU "Cargo eletivo ocupado" contém "Diretor" E "Conselheiro"
               Exemplo: "Conselheiro(Efetivo) e Dir. Presidente"

            ═══════════════════════════════════════════════════════════════
            📋 MEMBROS DE COMITÊS (seção 7.4):
            ═══════════════════════════════════════════════════════════════
            
            Procure seção "Comitês:" após os dados da pessoa
            Tabela tem: "Tipo comitê", "Cargo ocupado", "Data posse"
            ATENÇÃO: Pessoa pode estar em Comitê E ser Conselheiro (se tiver ambas as seções)
            Se pergunta sobre "membros do Comitê que são conselheiros":
               → Conte APENAS quem aparece em "Comitês:" E tem "Órgão da Administração" = "Conselho de Administração"
            
            ═══════════════════════════════════════════════════════════════
            
            INSTRUÇÕES: %s
            OBSERVAÇÕES: %s
            
            FORMATO DE RESPOSTA: NÚMERO (Nome1, Nome2, Nome3)
            Exemplo: "3 (João Silva, Maria Santos, Pedro Oliveira)"
            Se for 0: retorne apenas "0"
            
            RESPOSTA:
            """,
            q.getQuestao(),
            context,
            q.getComoPreencher() != null ? q.getComoPreencher() : "",
            q.getObservacoes() != null ? q.getObservacoes() : ""
        );
    }
    
    /**
     * Prompt especializado para extração de texto específico.
     */
    private String buildTextPrompt(Question q, String context) {
        return String.format("""
            Você é um assistente especializado em extrair textos específicos de Formulários de Referência.
            
            TAREFA: Extrair o nome/texto EXATO conforme solicitado.
            
            QUESTÃO: %s
            
            LOCALIZAÇÃO: %s
            
            INSTRUÇÕES:
            %s
            
            DOCUMENTOS:
            %s
            
            REGRAS CRÍTICAS:
            1. Copie o texto EXATAMENTE como está no documento
            2. Remova formatação desnecessária (negrito, itálico)
            3. Mantenha a capitalização original
            4. Para firmas de auditoria: use o nome completo oficial
            5. Para políticas: extraia APENAS o nome da política (ex: "Política de Transações com Partes Relacionadas")
               - NÃO inclua explicações ou parágrafos completos
               - Se a questão pede o nome da política, retorne somente o título (máximo 150 caracteres)
            6. Se não encontrar: "INFORMAÇÃO NÃO ENCONTRADA"
            7. NÃO invente ou parafraseie - copie literalmente
            8. IMPORTANTE: Retorne texto CURTO e DIRETO - não retorne parágrafos longos
            
            RESPOSTA (apenas o texto):
            """,
            q.getQuestao(),
            q.getOnde() != null ? q.getOnde() : "FR",
            q.getComoPreencher() != null ? q.getComoPreencher() : "",
            context
        );
    }
    
    /**
     * Prompt especializado para questões de múltipla escolha.
     */
    private String buildMultipleChoicePrompt(Question q, String context) {
        return String.format("""
            Você é um assistente especializado em análise de Formulários de Referência.
            
            TAREFA: Escolher UMA das opções pré-definidas baseado no documento.
            
            QUESTÃO: %s
            
            LOCALIZAÇÃO: %s
            
            OPÇÕES DISPONÍVEIS:
            %s
            
            OBSERVAÇÕES:
            %s
            
            DOCUMENTOS:
            %s
            
            REGRAS CRÍTICAS:
            1. Retorne APENAS o texto EXATO de uma das opções listadas
            2. NÃO adicione texto explicativo
            3. Escolha a opção que melhor descreve o que está no documento
            4. Se o documento afirma que NÃO possui/oferece algo: escolha opção "Não"
            5. Se não encontrar informação clara ou o documento não menciona: escolha "Não Divulgado"
            6. Leia com atenção todas as opções antes de decidir
            7. Frases como "não aplicável" ou "não oferece" significam "Não"
            
            RESPOSTA (apenas uma das opções):
            """,
            q.getQuestao(),
            q.getOnde() != null ? q.getOnde() : "FR",
            q.getComoPreencher() != null ? q.getComoPreencher() : "",
            q.getObservacoes() != null ? q.getObservacoes() : "",
            context
        );
    }
    
    /**
     * Prompt genérico para questões sem tipo definido.
     */
    private String buildGenericPrompt(Question q, String context) {
        return String.format("""
            Você é um assistente especializado em análise de Formulários de Referência.
            
            TAREFA: Extrair informação EXATA do documento fornecido.
            
            QUESTÃO: %s
            
            LOCALIZAÇÃO: %s
            
            INSTRUÇÕES:
            %s
            
            OBSERVAÇÕES:
            %s
            
            DOCUMENTOS:
            %s
            
            REGRAS:
            - Busque EXATAMENTE os termos mencionados
            - Retorne APENAS a informação solicitada
            - Se não encontrar: "INFORMAÇÃO NÃO ENCONTRADA"
            
            RESPOSTA:
            """,
            q.getQuestao(),
            q.getOnde() != null ? q.getOnde() : "FR",
            q.getComoPreencher() != null ? q.getComoPreencher() : "",
            q.getObservacoes() != null ? q.getObservacoes() : "",
            context
        );
    }
    
    /**
     * Pós-processa resposta aplicando regras específicas por tipo.
     * 
     * ESTRATÉGIA:
     * - Extrai apenas o valor relevante
     * - Remove textos explicativos indesejados
     * - Aplica formatação padronizada
     */
    private String postProcessAnswer(String rawAnswer, Question q) {
        if (rawAnswer == null || rawAnswer.trim().isEmpty()) {
            return "INFORMAÇÃO NÃO ENCONTRADA";
        }
        
        String processed = rawAnswer.trim();
        
        // Aplicar pós-processamento específico por tipo
        switch (q.getTipo()) {
            case MONETARIA:
                processed = postProcessMonetary(processed);
                break;
            case SIM_NAO:
                processed = postProcessYesNo(processed);
                break;
            case CONTAGEM:
                processed = postProcessCounting(processed);
                break;
            case TEXTO_ESPECIFICO:
                processed = postProcessText(processed);
                break;
            case MULTIPLA_ESCOLHA:
                processed = postProcessMultipleChoice(processed, q);
                break;
            default:
                processed = cleanGenericAnswer(processed);
        }
        
        return processed;
    }
    
    /**
     * Pós-processa respostas monetárias.
     * Extrai número + unidade, aplica multiplicação se necessário.
     */
    private String postProcessMonetary(String answer) {
        // Sempre delegar para applyMonetaryRules, que agora preserva o sinal negativo
        // e aplica formatação consistente. Isso evita casos em que o '-' é perdido
        // quando o sinal aparece antes ou depois do símbolo monetário.
        return applyMonetaryRules(answer);
    }
    
    /**
     * Pós-processa respostas SIM/NÃO.
     * Extrai APENAS "SIM", "NÃO", "NÃO DIVULGADO" ou "NÃO APLICADO".
     */
    private String postProcessYesNo(String answer) {
        String upperAnswer = answer.toUpperCase();
        
        // Remover pontuação
        upperAnswer = upperAnswer.replaceAll("[.!?;,]", "").trim();
        
        // Extrair resposta pura (remover texto explicativo)
        // Padrões comuns: "SIM = ...", "NÃO - ...", "SIM, pois...", etc.
        if (upperAnswer.matches("SIM[\\s=\\-:,].*")) {
            return "SIM";
        }
        if (upperAnswer.matches("NÃO[\\s=\\-:,].*")) {
            return "NÃO";
        }
        if (upperAnswer.contains("NÃO DIVULGADO") || upperAnswer.contains("NAO DIVULGADO")) {
            return "NÃO DIVULGADO";
        }
        if (upperAnswer.contains("NÃO APLICADO") || upperAnswer.contains("NAO APLICADO") || 
            upperAnswer.contains("NÃO SE APLICA") || upperAnswer.contains("NAO SE APLICA")) {
            return "NÃO APLICADO";
        }
        
        // Se resposta é apenas "SIM" ou "NÃO" (sem texto adicional)
        if (upperAnswer.equals("SIM")) {
            return "SIM";
        }
        if (upperAnswer.equals("NÃO") || upperAnswer.equals("NAO")) {
            return "NÃO";
        }
        
        // Se começar com SIM ou NÃO, extrair
        if (upperAnswer.startsWith("SIM")) {
            return "SIM";
        }
        if (upperAnswer.startsWith("NÃO") || upperAnswer.startsWith("NAO")) {
            return "NÃO";
        }
        
        // Fallback: se contém afirmação
        if (upperAnswer.contains("POSSUI") || upperAnswer.contains("DIVULGA") || 
            upperAnswer.contains("INSTALADO") || upperAnswer.contains("ADEQUADO")) {
            return "SIM";
        }
        if (upperAnswer.contains("NÃO POSSUI") || upperAnswer.contains("NÃO DIVULGA") || 
            upperAnswer.contains("NÃO INSTALADO") || upperAnswer.contains("NÃO ADEQUADO")) {
            return "NÃO";
        }
        
        return "INFORMAÇÃO NÃO ENCONTRADA";
    }
    
    /**
     * Pós-processa respostas de contagem.
     * Extrai número e preserva nomes se presentes.
     * Formato esperado: "NÚMERO (Nome 1, Nome 2, ...)"
     */
    private String postProcessCounting(String answer) {
        // Se é "INFORMAÇÃO NÃO ENCONTRADA", manter
        if (answer.toUpperCase().contains("INFORMAÇÃO NÃO ENCONTRADA") || 
            answer.toUpperCase().contains("INFORMACAO NAO ENCONTRADA")) {
            return "INFORMAÇÃO NÃO ENCONTRADA";
        }
        
        // Remover pontos finais e espaços extras
        answer = answer.trim().replaceAll("\\.$", "");
        
        // Verificar se já está no formato "NÚMERO (Nomes...)"
        Pattern formatPattern = Pattern.compile("^(\\d+)\\s*\\(([^)]+)\\)");
        Matcher formatMatcher = formatPattern.matcher(answer);
        
        if (formatMatcher.find()) {
            // Já está no formato correto
            String numero = formatMatcher.group(1);
            String nomes = formatMatcher.group(2).trim();
            return numero + " (" + nomes + ")";
        }
        
        // Se é apenas um número puro, retornar
        if (answer.matches("^\\d+$")) {
            return answer;
        }
        
        // Extrair primeiro número da resposta (fallback para formato antigo)
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(answer);
        
        if (matcher.find()) {
            return matcher.group();
        }
        
        // Se não encontrou número, verificar se há indicação de zero
        String upperAnswer = answer.toUpperCase();
        if (upperAnswer.contains("NENHUM") || upperAnswer.contains("ZERO") || 
            upperAnswer.contains("NÃO HÁ") || upperAnswer.contains("NAO HA")) {
            return "0";
        }
        
        return "INFORMAÇÃO NÃO ENCONTRADA";
    }
    
    /**
     * Pós-processa respostas de texto específico.
     * Limpa formatação mas preserva conteúdo.
     */
    private String postProcessText(String answer) {
        // Remover aspas desnecessárias
        answer = answer.replaceAll("^\"|\"$", "");
        
        // Remover múltiplos espaços
        answer = answer.replaceAll("\\s+", " ");
        
        // Se contém "Política de", extrair apenas o nome da política
        if (answer.toLowerCase().contains("política de")) {
            // Procurar padrão "Política de [nome]"
            int start = answer.toLowerCase().indexOf("política de");
            if (start != -1) {
                String politica = answer.substring(start);
                // Extrair até o primeiro ponto, vírgula ou até 150 chars
                int endPeriod = politica.indexOf(".");
                int endComma = politica.indexOf(",");
                int end = politica.length();
                
                if (endPeriod != -1 && endPeriod < end) end = endPeriod;
                if (endComma != -1 && endComma < end) end = endComma;
                if (end > 150) end = 150;
                
                answer = politica.substring(0, end).trim();
            }
        }
        
        // Limpar se for muito longo (> 200 chars = texto explicativo indesejado)
        if (answer.length() > 200) {
            // Tentar extrair apenas primeira linha ou primeira frase
            String[] lines = answer.split("\n");
            if (lines.length > 0 && lines[0].length() < 150) {
                answer = lines[0];
            } else {
                String[] sentences = answer.split("\\.");
                if (sentences.length > 0 && sentences[0].length() < 150) {
                    answer = sentences[0];
                }
            }
        }
        
        return answer.trim();
    }
    
    /**
     * Pós-processa respostas de múltipla escolha.
     * Valida se resposta está entre as opções do guia.
     */
    private String postProcessMultipleChoice(String answer, Question q) {
        // Extrair opções do campo "Como Preencher"
        String comoPreencher = q.getComoPreencher();
        if (comoPreencher == null) {
            return answer.trim();
        }
        
        // Normalizar resposta
        String normalizedAnswer = answer.trim();
        
        // Para Q47 (Seguro D&O): validar opções específicas
        if (q.getNumero() == 47) {
            String upperAnswer = answer.toUpperCase();
            if (upperAnswer.contains("SEGURO D&O") || upperAnswer.contains("D&O")) {
                return "Seguro D&O";
            }
            if (upperAnswer.contains("OUTRA FORMA") || upperAnswer.contains("REEMBOLSO")) {
                return "Outra forma de reembolso";
            }
            if (upperAnswer.contains("NÃO DIVULGADO") || upperAnswer.contains("NAO DIVULGADO")) {
                return "Não Divulgado";
            }
            if (upperAnswer.equals("NÃO") || upperAnswer.equals("NAO")) {
                return "Não";
            }
        }
        
        return normalizedAnswer;
    }
    
    /**
     * Limpeza genérica de respostas.
     */
    private String cleanGenericAnswer(String answer) {
        // Remover múltiplos espaços e quebras de linha
        answer = answer.replaceAll("\\s+", " ").trim();
        
        // Remover ponto final se único
        if (answer.endsWith(".") && !answer.contains(". ")) {
            answer = answer.substring(0, answer.length() - 1);
        }
        
        return answer;
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
        Pattern pattern = Pattern.compile("([\\d.,]+)\\s*(?:\\()?(?:em)?\\s*R?\\$?\\s*(mil|milh\\u00e3o|milh\\u00f5es|thousand|million)?(?:\\))?", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(value);

        if (matcher.find()) {
            String numberStr = matcher.group(1);
            String unit = matcher.group(2);

            try {
                // Detectar se há sinal negativo imediatamente antes do número (ex: "-1.234", "R$ -1.234", "-R$ 1.234")
                boolean negative = false;
                int numStart = matcher.start(1);
                // Procurar para trás até o primeiro caractere não espaço para ver se é '-' ou unicode minus
                int idx = numStart - 1;
                while (idx >= 0 && Character.isWhitespace(value.charAt(idx))) idx--;
                if (idx >= 0) {
                    char c = value.charAt(idx);
                    if (c == '-' || c == '−') {
                        negative = true;
                    }
                }

                // Remover pontos de milhar e trocar vírgula por ponto
                String cleanNumber = numberStr.replace(".", "").replace(",", ".");
                double number = Double.parseDouble(cleanNumber);

                // Aplicar multiplicação conforme unidade
                if (unit != null) {
                    if (unit.toLowerCase().contains("mil") || unit.equalsIgnoreCase("thousand")) {
                        number *= 1000;
                    } else if (unit.toLowerCase().contains("milh") || unit.equalsIgnoreCase("million")) {
                        number *= 1000000;
                    }
                }

                long rounded = Math.round(number);
                String formatted = formatCurrency(rounded);
                // Reinserir sinal negativo se detectado
                if (negative) {
                    return "-" + formatted;
                }
                return formatted;

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
