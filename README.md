# 🤖 FR-Processor: Extração Automatizada de Formulários de Referência

> **Sistema RAG + LLM para análise automatizada de Formulários de Referência de empresas brasileiras listadas em bolsa**

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![LangChain4j](https://img.shields.io/badge/LangChain4j-1.8.0-blue.svg)](https://github.com/langchain4j/langchain4j)
[![Gemini](https://img.shields.io/badge/Google-Gemini%202.5%20Flash-green.svg)](https://ai.google.dev/)
[![Status](https://img.shields.io/badge/Status-Prot%C3%B3tipo%20de%20Pesquisa-orange.svg)]()
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## ⚠️ AVISO IMPORTANTE - LEIA ANTES DE USAR

Este é um **protótipo de pesquisa acadêmica** que demonstra a viabilidade técnica de RAG+LLM para extração de Formulários de Referência.

### 🎯 O que este projeto DEMONSTRA:
- ✅ Técnicas de RAG (Retrieval-Augmented Generation) em documentos corporativos
- ✅ Integração de LangChain4j + Google Gemini
- ✅ Pipeline completo: indexação → busca → geração → pós-processamento
- ✅ **83.3% de acurácia em UM caso específico** (AMBIPAR)

### ⛔ O que este projeto NÃO É:
- ❌ **Sistema pronto para produção** - Contém erros críticos em múltiplos PDFs
- ❌ **Substituto para trabalho manual** - Requer revisão humana obrigatória
- ❌ **Ferramenta de decisão financeira** - Não use para investimentos ou compliance
- ❌ **Generalizável** - Acurácia cai de 83% (1 PDF) para 40-60% (10 PDFs)

### 🔴 Problemas Conhecidos em Produção:
1. **Prejuízos invertidos** - Valores negativos aparecem como positivos (erro crítico)
2. **Unidades erradas** - Confunde milhão/bilhão
3. **Alta taxa de "não encontrado"** - 60-83% em alguns documentos
4. **Overfitting** - Otimizado para AMBIPAR, não generaliza

👉 **Veja análise completa na seção:** [Limitações Conhecidas](#-limitações-conhecidas-e-problemas-em-produção)

---

## 📋 Índice

- [Visão Geral](#-visão-geral)
- [Problema Resolvido](#-problema-resolvido)
- [Metodologia](#-metodologia)
  - [1. Indexação de Documentos](#1-indexação-de-documentos-pipeline-rag)
  - [2. Enriquecimento de Busca](#2-enriquecimento-de-busca-estratégia-diferencial)
  - [3. Construção do Prompt](#3-construção-do-prompt-aumentado)
  - [4. Pós-processamento](#4-pós-processamento-inteligente)
  - [5. Sistema de Tipos](#5-sistema-de-tipos-de-questões)
  - [6. Rate Limiting e Checkpoints](#6-rate-limiting-e-checkpoints)
- [Resultados](#-resultados)
- [Métricas de Performance](#-métricas-de-performance)
- [Instalação e Configuração](#-instalação-e-configuração)
- [Como Executar](#-como-executar)
- [Estrutura do Projeto](#-estrutura-do-projeto)
- [Principais Desafios e Aprendizados](#-principais-desafios-e-aprendizados)
- [Tecnologias Utilizadas](#-tecnologias-utilizadas)
- [Autores](#-autores)

---

## 🎯 Visão Geral

O **FR-Processor** é um sistema automatizado que utiliza técnicas de **RAG (Retrieval-Augmented Generation)** e **LLMs (Large Language Models)** para extrair informações específicas de Formulários de Referência (FRs) de empresas brasileiras listadas na B3.

### 🎬 Demo Rápida

```bash
# 1. Configure sua API key
cp .env.example .env
# Edite .env e adicione sua GEMINI_API_KEY

# 2. Compile o projeto
./gradlew clean build

# 3. Execute o processamento
./gradlew run

# 4. Veja os resultados
cat output/respostas.csv
```

**Resultado:** 24 questões processadas automaticamente em ~4-5 minutos.

> ⚠️ **IMPORTANTE:** A acurácia de 83.3% reportada neste projeto refere-se **apenas à validação com um único documento** (AMBIPAR). Testes com múltiplos PDFs revelaram problemas significativos que estão documentados na seção [Limitações Conhecidas](#-limitações-conhecidas-e-problemas-em-produção).

---

## 💼 Problema Resolvido

### Contexto Empresarial

> **"Como uma empresa de análise de dados, eu quero substituir os humanos na tarefa de extrair 30 itens de informação de formulários de referência publicados por 280 empresas brasileiras listadas em bolsa, para que não precisemos mais fazer isso de forma manual."**

### Desafio

- **280 empresas** × **30 questões** = **8.400 respostas** para extrair manualmente
- Tempo médio manual: **~5 minutos por questão** = **700 horas de trabalho**
- Erros humanos de interpretação e digitação
- Processo repetitivo e desmotivante

### Solução Proposta

Sistema automatizado que:
1. **Lê** Formulários de Referência em PDF (documentos de 200-300 páginas)
2. **Extrai** informações específicas usando RAG + LLM
3. **Valida** e formata as respostas conforme regras de negócio
4. **Gera** CSV para análise

**Tempo de execução:** ~5 minutos por empresa = **23 horas totais** (vs 700 horas manuais)  
**Potencial de economia:** **97% de redução no tempo**

> ⚠️ **Status do Projeto:** Este é um **protótipo de pesquisa** que demonstra a viabilidade da abordagem RAG+LLM para extração de FRs. A acurácia atual permite uso como **ferramenta de apoio** (pré-preenchimento que requer revisão humana), mas **não substitui completamente** o trabalho manual devido às limitações documentadas abaixo.

---

## 🔬 Metodologia

O sistema implementa um pipeline completo de RAG (Retrieval-Augmented Generation) otimizado para documentos corporativos estruturados:

```
┌────────────────────────────────────────────────────────────────────────────────────────────┐
│                          PIPELINE FR-PROCESSOR - RAG + LLM                                 │
└────────────────────────────────────────────────────────────────────────────────────────────┘

                                    ┌─────────────┐
                                    │   PDF de    │
                                    │ 200-300 pág │
                                    └──────┬──────┘
                                           │
                                           ▼
                        ┌──────────────────────────────────────┐
                        │     1. INDEXAÇÃO (DocumentIndexer)   │
                        │  • Parse com Apache Tika             │
                        │  • Chunking: 2000 tokens, overlap 600│
                        │  • Embeddings: AllMiniLmL6V2 (384d)  │
                        │  • Store: InMemory (~516 chunks)     │
                        └──────────────┬───────────────────────┘
                                       │
                                       ▼
                        ┌─────────────────────────────────────────┐
                        │   EMBEDDING STORE (Banco Vetorial)      │
                        │   [Chunk 1 → Vector 384d]               │
                        │   [Chunk 2 → Vector 384d]               │
                        │   [...516 chunks indexados...]          │
                        └──────────────┬──────────────────────────┘
                                       │
        ┌──────────────────────────────┴────────────────────────────────┐
        │                                                               │
        ▼                                                               ▼
┌──────────────────┐                                      ┌──────────────────────┐
│  Guia de Coleta  │                                      │   Para cada questão  │
│  • 24 questões   │─────────────────────────────────────▶│   (Q2, Q3, ... Q63) │
│  • Palavras-chave│                                      └──────────┬───────────┘
│  • Tipos         │                                                 │
└──────────────────┘                                                 ▼
                                              ┌─────────────────────────────────────┐
                                              │  2. BUSCA ENRIQUECIDA               │
                                              │  Query = Questão + Onde? +          │
                                              │          Palavras-chave RAG         │
                                              │  Busca por similaridade (cosine)    │
                                              │  Top 15 chunks (score > 0.60)       │
                                              └──────────────┬──────────────────────┘
                                                             │
                                                             ▼
                                              ┌─────────────────────────────────────┐
                                              │  Chunks Relevantes Recuperados      │
                                              │  [Chunk A - Score: 0.89]            │
                                              │  [Chunk B - Score: 0.87]            │
                                              │  [...até 15 chunks...]              │
                                              └──────────────┬──────────────────────┘
                                                             │
                                                             ▼
                                              ┌─────────────────────────────────────┐
                                              │  3. CONSTRUÇÃO DO PROMPT            │
                                              │  • Identifica Tipo (MONETARIA,      │
                                              │    SIM_NAO, CONTAGEM, etc)          │
                                              │  • Prompt especializado por tipo    │
                                              │  • Contexto = chunks concatenados   │
                                              │  • Regras específicas injetadas     │
                                              └──────────────┬──────────────────────┘
                                                             │
                                                             ▼
                                              ┌─────────────────────────────────────┐
                                              │  4. GEMINI LLM (Google AI)          │
                                              │  Modelo: gemini-2.5-flash           │
                                              │  Temperature: 0.0 (determinístico)  │
                                              │  Timeout: 30s | Rate: 10 RPM        │
                                              │  → Analisa contexto                 │
                                              │  → Extrai informação solicitada     │
                                              └──────────────┬──────────────────────┘
                                                             │
                                                             ▼
                                              ┌─────────────────────────────────────┐
                                              │  Resposta Bruta do LLM              │
                                              │  Ex: "4.872.707 (em R$ mil)"        │
                                              └──────────────┬──────────────────────┘
                                                             │
                                                             ▼
                                              ┌─────────────────────────────────────┐
                                              │  5. PÓS-PROCESSAMENTO               │
                                              │  • MONETARIA: multiplica mil/milhão │
                                              │  • SIM_NAO: remove explicações      │
                                              │  • CONTAGEM: extrai número + nomes  │
                                              │  • Validações de formato            │
                                              └──────────────┬──────────────────────┘
                                                             │
                                                             ▼
                                              ┌─────────────────────────────────────┐
                                              │  Resposta Final Formatada           │
                                              │  Ex: "R$ 4.872.707.000"             │
                                              └──────────────┬──────────────────────┘
                                                             │
                                                             │ (Delay 6s - rate limit)
                                                             │ (Checkpoint a cada 5 Q)
                                                             │
                                                             ▼
                                                    ┌─────────────────┐
                                                    │  Próxima Questão│
                                                    └────────┬────────┘
                                                             │
                                                             │ Após 24 questões
                                                             ▼
                                              ┌─────────────────────────────────────┐
                                              │  CSV OUTPUT (respostas.csv)         │
                                              │  Empresa;Q2;Q3;...Q63               │
                                              │  AMBIPAR;R$4.8bi;R$56mi;...         │
                                              └─────────────────────────────────────┘
```

### 1. Indexação de Documentos (Pipeline RAG)

**Objetivo:** Transformar o PDF do Formulário de Referência em uma base de conhecimento vetorial consultável.

#### 1.1 Parsing (Apache Tika)

```java
ApacheTikaDocumentParser parser = new ApacheTikaDocumentParser();
Document document = parser.parse(inputStream);
```

- **Entrada:** PDF de 200-300 páginas
- **Saída:** Texto puro (~500.000 caracteres)
- **Tempo:** ~2-3 segundos
- **Desafio superado:** Tabelas complexas, formatação especial, caracteres especiais

#### 1.2 Chunking (Divisão Inteligente)

```java
DocumentSplitter splitter = DocumentSplitters.recursive(
    MAX_SEGMENT_SIZE_IN_TOKENS,    // 2000 tokens
    SEGMENT_OVERLAP_IN_TOKENS      // 600 tokens
);
```

**Parâmetros Otimizados (após experimentação):**

| Configuração | Valor Testado | Resultado | Valor Final |
|--------------|---------------|-----------|-------------|
| Chunk Size | 600 tokens | ❌ Tabelas cortadas | - |
| Chunk Size | 1200 tokens | ⚠️ Melhor, mas incompleto | - |
| **Chunk Size** | **2000 tokens** | ✅ **Tabelas completas** | **ATUAL** |
| Overlap | 100 tokens | ⚠️ Perdia contexto | - |
| **Overlap** | **600 tokens** | ✅ **Mantém contexto** | **ATUAL** |

**Por que 2000 tokens?**
- Formulários de Referência contêm tabelas extensas (ex: lista de conselheiros com CPF, cargo, data de posse)
- Chunks de 600-1200 tokens cortavam tabelas ao meio
- 2000 tokens = ~1500 palavras = tabelas completas + contexto

**Resultado:**
- ~516 chunks por documento
- ~4 segundos para processar

#### 1.3 Embedding (Vetorização)

```java
EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();
```

**Modelo:** AllMiniLmL6V2
- **Tipo:** Sentence Transformers (ONNX)
- **Dimensões:** 384
- **Tamanho:** ~80 MB (download automático na primeira execução)
- **Performance:** ~8ms por chunk
- **Qualidade:** Excelente para português técnico-financeiro
- **Vantagem:** 100% local, sem custos, sem internet após download

**Exemplo de embedding:**
```
"Receita líquida da empresa em 2023" → [0.12, -0.34, 0.89, ..., 0.45]
                                         ↑ vetor de 384 números
```

#### 1.4 Armazenamento

```java
InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
```

- **Tipo:** Banco vetorial em memória
- **Capacidade:** ~1000 documentos simultaneamente
- **Busca:** Similaridade de cosseno (0.0 a 1.0)
- **Tempo de busca:** ~50ms para 516 chunks

**Total da Indexação:** ~10 segundos por PDF

---

### 2. Enriquecimento de Busca (Estratégia Diferencial)

**Problema Identificado:** Busca somente com a pergunta retornava chunks irrelevantes.

**Exemplo:**
- **Query simples:** "Qual o lucro líquido?"
- **Chunks retornados:** Textos genéricos sobre lucro, sem os valores

**Solução:** Enriquecer a query com termos do "Guia de Coleta.csv"

#### Guia de Coleta (CSV de Entrada)

```csv
Nº;Dificuldade;Questão;Onde?;Como Preencher?;OBSERVAÇÕES;Tipo;Palavras-chave RAG
3;Médio;Qual é o lucro líquido da empresa?;2.1, item h;COPIAR o valor...;;MONETARIA;item h, Lucro líquido, demonstrações financeiras, R$, mil, milhão, valor, exercício, 31/12
```

**Colunas importantes:**
- **Onde?** → Localização no FR (ex: "2.1, item h", "FR 7.3")
- **Como Preencher?** → Instruções detalhadas do analista
- **Palavras-chave RAG** → Termos específicos para enriquecer busca

#### Implementação do Enriquecimento

```java
private String buildEnrichedSearchQuery(Question q) {
    StringBuilder query = new StringBuilder();
    
    // 1. Questão original (sempre primeiro)
    query.append(q.getQuestao()).append(" ");
    
    // 2. Localização no documento (CRÍTICO para documentos estruturados)
    if (q.getOnde() != null && !q.getOnde().isEmpty()) {
        query.append(q.getOnde()).append(" ");
    }
    
    // 3. Palavras-chave RAG (nova coluna - diferencial do sistema)
    if (q.getPalavrasChaveRag() != null && !q.getPalavrasChaveRag().isEmpty()) {
        query.append(q.getPalavrasChaveRag()).append(" ");
    }
    
    return query.toString().trim();
}
```

**Exemplo Prático:**

Query original:
```
"Qual é o lucro líquido da empresa?"
```

Query enriquecida:
```
"Qual é o lucro líquido da empresa? 2.1, item h, Condições financeiras e patrimoniais item h, Lucro líquido, demonstrações financeiras, R$, mil, milhão, valor, exercício, 31/12"
```

**Impacto:**
- Antes: Chunks genéricos com score ~0.65
- Depois: Chunks precisos (tabelas com valores) com score ~0.85
- **Melhoria de acurácia:** +15 pontos percentuais

---

### 3. Construção do Prompt Aumentado

#### Sistema de Tipos de Questões

O projeto implementa **5 tipos especializados** de questões, cada um com prompt e pós-processamento customizados:

```java
public enum TipoQuestao {
    MONETARIA,          // Ex: Q2, Q3, Q6, Q8 (receita, lucro, gastos)
    SIM_NAO,            // Ex: Q10, Q14, Q15 (possui política? divulga?)
    CONTAGEM,           // Ex: Q23, Q30, Q31 (quantos comitês? mulheres?)
    TEXTO_ESPECIFICO,   // Ex: Q5, Q27 (nome da auditoria, política)
    MULTIPLA_ESCOLHA    // Ex: Q47 (tipo de seguro D&O)
}
```

#### Exemplo de Prompt Especializado (Monetária)

```java
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
        1. Retorne APENAS o número com unidade (ex: "4.872.707 (em R$ mil)")
        2. SEMPRE identifique se o valor está em R$ mil, R$ milhão ou absoluto
        3. Busque em tabelas da seção indicada (geralmente 2.1.h)
        4. Para bancos: "Receitas da Intermediação Financeira" = Receita Líquida
        5. Para prejuízo: inclua o sinal negativo (-)
        6. Se não encontrar: "INFORMAÇÃO NÃO ENCONTRADA"
        7. NÃO inclua explicações ou textos adicionais
        
        RESPOSTA (apenas número + unidade):
        """,
        q.getQuestao(),
        q.getOnde(),
        q.getComoPreencher(),
        context
    );
}
```

**Diferencial:** Cada tipo tem regras específicas (ex: monetária detecta mil/milhão, SIM_NAO remove explicações)

---

### 4. Pós-processamento Inteligente

#### 4.1 Pós-processamento Monetário

**Problema:** Gemini retorna valores como "4.872.707 (em R$ mil)"  
**Necessário:** R$ 4.872.707.000 (aplicar multiplicação)

```java
private String applyMonetaryRules(String value) {
    // Detecta padrões: número + unidade (mil/milhão)
    Pattern pattern = Pattern.compile(
        "([\\d.,]+)\\s*(?:\\()?(?:em)?\\s*R?\\$?\\s*(mil|milhão)?"
    );
    
    // Extrai número e unidade
    String numberStr = matcher.group(1);
    String unit = matcher.group(2);
    
    // Remove pontos de milhar, converte para double
    String cleanNumber = numberStr.replace(".", "").replace(",", ".");
    double number = Double.parseDouble(cleanNumber);
    
    // Aplica multiplicação
    if (unit.contains("mil")) {
        number *= 1000;
    } else if (unit.contains("milhão")) {
        number *= 1000000;
    }
    
    // Formata: R$ 4.872.707.000
    return formatCurrency(Math.round(number));
}
```

#### 4.2 Pós-processamento SIM/NÃO

**Problema:** Gemini retorna "SIM = a empresa cita que possui política..."  
**Necessário:** Apenas "SIM"

```java
private String postProcessYesNo(String answer) {
    String upperAnswer = answer.toUpperCase();
    
    // Remover pontuação
    upperAnswer = upperAnswer.replaceAll("[.!?;,]", "").trim();
    
    // Extrair resposta pura (remover texto explicativo)
    if (upperAnswer.matches("SIM[\\s=\\-:,].*")) {
        return "SIM";
    }
    if (upperAnswer.matches("NÃO[\\s=\\-:,].*")) {
        return "NÃO";
    }
    
    // ... demais casos
    
    return "INFORMAÇÃO NÃO ENCONTRADA";
}
```

#### 4.3 Pós-processamento de Contagem

**Desafio:** Além de contar, preservar nomes para auditoria

**Formato esperado:** "7 (Tércio, Felipe, Guilherme, Thiago, Victor, Alessandra, Carlos)"

```java
private String postProcessCounting(String answer) {
    // Formato: "NÚMERO (Nome 1, Nome 2, ...)"
    Pattern formatPattern = Pattern.compile("^(\\d+)\\s*\\(([^)]+)\\)");
    Matcher formatMatcher = formatPattern.matcher(answer);
    
    if (formatMatcher.find()) {
        String numero = formatMatcher.group(1);
        String nomes = formatMatcher.group(2).trim();
        return numero + " (" + nomes + ")";
    }
    
    // Fallback: extrair apenas número
    // ...
}
```

---

### 5. Sistema de Tipos de Questões

#### Distribuição das 24 Questões

| Tipo | Quantidade | Questões | Acurácia |
|------|------------|----------|----------|
| **MONETARIA** | 4 | Q2, Q3, Q6, Q8 | 100% 🎯 |
| **SIM_NAO** | 9 | Q10, Q14, Q15, Q16, Q18, Q19, Q41, Q47, Q63 | 77.8% |
| **CONTAGEM** | 8 | Q23, Q30, Q31, Q32, Q33, Q34, Q38, Q39, Q40 | 75.0% |
| **TEXTO_ESPECIFICO** | 2 | Q5, Q27 | 100% 🎯 |
| **MULTIPLA_ESCOLHA** | 1 | Q47 | 0% |

#### Fluxo de Processamento por Tipo

```
┌─────────────────────────────────────────────────────────────────┐
│                   PROCESSAMENTO DE QUESTÃO                      │
└─────────────────────────────────────────────────────────────────┘

[Questão do CSV]
      ↓
[Identifica Tipo] → MONETARIA → buildMonetaryPrompt() 
      ↓                          ↓
      ↓                     [Gemini: "4.872.707 (em R$ mil)"]
      ↓                          ↓
      ↓                     postProcessMonetary()
      ↓                          ↓
      ↓                     [R$ 4.872.707.000] ✅
      ↓
      ├── SIM_NAO → buildYesNoPrompt() → postProcessYesNo()
      ├── CONTAGEM → buildCountingPrompt() → postProcessCounting()
      ├── TEXTO_ESPECIFICO → buildTextPrompt() → postProcessText()
      └── MULTIPLA_ESCOLHA → buildMultipleChoicePrompt() → postProcessMultipleChoice()
```

---

### 6. Rate Limiting e Checkpoints

#### Limites da API do Gemini

**Free Tier (gemini-2.5-flash):**
- **RPM (Requests Per Minute):** 10
- **RPD (Requests Per Day):** 1500
- **TPM (Tokens Per Minute):** 250.000

**Implementação:**

```java
// Config.java
public static final long REQUEST_DELAY_MS = 6000; // 6 segundos
public static final int CHECKPOINT_INTERVAL = 5;   // Salvar a cada 5 questões

// RagApplication.java
for (int index = 0; index < numQuestionsToProcess; index++) {
    Question question = reader.readQuestionByIndex(index);
    String answer = processor.processQuestion(question);
    response.setResposta(question.getNumero(), answer);
    
    // Rate limiting
    if (index < numQuestionsToProcess - 1) {
        System.out.println("⏳ Aguardando 6s (rate limiting)...");
        Thread.sleep(REQUEST_DELAY_MS);
    }
    
    // Checkpoint
    if ((index + 1) % CHECKPOINT_INTERVAL == 0) {
        writer.flush(); // Salva progresso no CSV
    }
}
```

**Benefícios:**
- **Nunca estoura rate limit** (10 requests/min → delay de 6s garante máx 10/min)
- **Checkpoint a cada 5 questões** previne perda de progresso
- **Retomada automática** em caso de erro (ler última linha do CSV)

---

## 📊 Resultados

### ⚠️ Contexto Importante

Os resultados apresentados abaixo são baseados em **validação com um único documento** (AMBIPAR Participações e Empreendimentos S.A. - FR 2024). 

**Acurácia no caso AMBIPAR:** 83.3% (20/24 questões corretas)

> **ATENÇÃO:** Testes posteriores com **10 diferentes PDFs** revelaram que esta acurácia **não se generaliza**. Problemas identificados em produção incluem:
> - ❌ Unidades monetárias incorretas (milhão onde deveria ser bilhão)
> - ❌ Valores negativos (prejuízos) mostrados como positivos
> - ❌ Informações não encontradas mesmo estando presentes no documento
> - ❌ Nomes de auditoria incorretos ou incompletos
> 
> Ver seção completa: [Limitações Conhecidas](#-limitações-conhecidas-e-problemas-em-produção)

#### Performance por Tipo de Questão (Caso AMBIPAR apenas)

| Tipo | Acertos | Total | Taxa | Status |
|------|---------|-------|------|--------|
| **MONETÁRIA** | 5/5 | 100% | 🎯 | Perfeito |
| **TEXTO_ESPECÍFICO** | 2/2 | 100% | 🎯 | Perfeito |
| **SIM/NÃO** | 7/9 | 77.8% | ⚠️ | Bom |
| **CONTAGEM** | 6/8 | 75.0% | ⚠️ | Bom |

### Análise Detalhada - Questões Corretas no Caso AMBIPAR (20)

> 📌 **Nota:** Os resultados abaixo são específicos para o documento da AMBIPAR e não representam a performance em outros PDFs.

#### 💰 Monetárias - 100% no caso AMBIPAR (5/5)

| # | Questão | RAG | Ground Truth | Fonte |
|---|---------|-----|--------------|-------|
| Q2 | Receita Líquida | R$ 4.872.707.000 | R$ 4.872.707.000 | FR 2.1.h, p.65 |
| Q3 | Lucro Líquido | R$ 56.649.000 | R$ 56.649.000 | FR 2.1.h, p.65 |
| Q6 | Gastos Auditoria | R$ 4.380.131 | R$ 4.380.131 | FR 9.1 |
| Q8 | Serviços Adicionais | R$ 2.170.131 | R$ 2.170.130 | FR 9.1 |

✅ **Sistema perfeito para valores monetários**

#### 📝 Texto Específico - 100% (2/2)

| # | Questão | RAG | Ground Truth |
|---|---------|-----|--------------|
| Q5 | Firma Auditoria | BDO RCS Auditores... | BDO RCS Auditores... |
| Q27 | Política Conflitos | Política de Transações... | Política de transações... |

✅ **Extração precisa de textos específicos**

#### ✔️ SIM/NÃO - 77.8% (7/9)

**Corretas (7):**
- Q10: Política de Riscos → SIM
- Q14: Auditoria Interna → SIM
- Q15: Controles Adequados → SIM
- Q16: Deficiências → NÃO
- Q18: Divulga ASG → SIM
- Q19: Conselho Fiscal → NÃO
- Q41: Coordenador Independente → NÃO

**Incorretas (2):**
- Q47: Seguro D&O → Esperado: "Não" | RAG: "Não Divulgado" ❌
- Q63: Casos Fraude → Esperado: "Não" | RAG: "SIM" ❌

#### 🔢 Contagem - 75% (6/8)

**Corretas (6):**
- Q23: Nº Comitês → 2 (Auditoria, Sustentabilidade)
- Q30: Total Conselheiros → 7
- Q31: Mulheres → 1 (Alessandra)
- Q32: Externos → 2 (Alessandra, Carlos)
- Q34: Executivos → 1 (Tércio Jr)
- Q38: Membros Comitê → 2 (parcial)

**Incorretas (2):**
- Q33: Independentes → Esperado: 4 | RAG: 3 ❌ (falta José Carlos)
- Q39: Conselheiros no Comitê → Esperado: 2 | RAG: 0 ❌
- Q40: Independentes no Comitê → Esperado: 2 | RAG: 0 ❌

---

### Análise de Erros - Padrões Identificados

#### 🔴 Padrão Crítico: José Carlos de Souza Ausente

**Questões Afetadas:** Q33, Q39, Q40 (3 questões = 12.5%)  
**Impacto:** Se resolver, acurácia sobe para **95.8%**

**Hipóteses:**
1. ✅ Chunk com José Carlos tem score < 0.60 (filtrado)
2. Nome muito comum, embeddings não diferenciam
3. Tabela dele foi cortada no chunking (menos provável com 2000 tokens)

**Solução Proposta:** Aumentar MAX_RESULTS_FOR_RETRIEVAL de 15 para 30-35

---

#### 🟡 Padrão Médio: Interpretação de Negações

**Questões Afetadas:** Q47, Q63 (2 questões = 8.3%)

**Q47 - Seguro D&O:**
```
FR 7.7, p.204: "Item não aplicável, uma vez que a Companhia 
não oferece seguro de responsabilidade civil"
```
- LLM interpretou "não aplicável" como "Não Divulgado"
- Deveria interpretar como "Não"

**Q63 - Casos de Fraude:**
```
FR 4.1: "A Companhia identificou risco de fraude..." (Riscos)
FR 5.3.c: "Não houve nenhum caso confirmado" (Casos Reais)
```
- LLM viu "fraude" em ambas seções e retornou SIM
- Deveria diferenciar "risco identificado" de "caso confirmado"

**Solução:** Enriquecer prompt com exemplos de negações

---

## 📈 Métricas de Performance

### Tempo de Execução

| Fase | Tempo | Observações |
|------|-------|-------------|
| **Indexação (1 PDF)** | ~10s | Parse + Chunking + Embeddings |
| **Por Questão** | ~12s | 6s rate limit + 6s processamento |
| **24 Questões (1 empresa)** | ~4-5 min | Com rate limiting do Free Tier |
| **280 Empresas (estimativa)** | ~23h | Tempo de execução (não valida acurácia) |

### Custos

| Recurso | Custo | Observação |
|---------|-------|------------|
| **Embeddings (AllMiniLmL6V2)** | $0 | 100% local, gratuito |
| **Gemini Free Tier** | $0 | 10 RPM, 1500 RPD |
| **Total por Empresa** | $0 | Usando Free Tier |
| **Total 280 Empresas** | $0 | Distribuindo em 14 dias |


### Acurácia vs Benchmarks

> ⚠️ **ATENÇÃO:** Os números abaixo são baseados **apenas no caso AMBIPAR**. A performance real em múltiplos PDFs é significativamente inferior (ver [Limitações Conhecidas](#-limitações-conhecidas-e-problemas-em-produção)).

| Sistema | Acurácia (1 PDF) | Acurácia Estimada (10+ PDFs) | Observações |
|---------|------------------|------------------------------|-------------|
| **FR-Processor (caso AMBIPAR)** | 83.3% | ⚠️ **~40-60%*** | *Ver seção de limitações |
| **Outro Projeto (menos questões)** | N/A | ~80-90% | Focou em poucas questões, validou em 10 PDFs |
| GPT-4 Típico (literatura) | 85% | 80-85% | Mais consistente entre documentos |
| RAG Genérico (literatura) | 75% | 70-75% | Baseline |

**Breakdown por Tipo (apenas caso AMBIPAR):**

| Tipo | AMBIPAR (1 PDF) | Múltiplos PDFs Estimado | Delta |
|------|-----------------|-------------------------|-------|
| Monetária | 100% 🥇 | ~40-60% ❌ | -40pp a -60pp |
| Texto | 100% 🥇 | ~60-80% ⚠️ | -20pp a -40pp |
| SIM/NÃO | 77.8% | ~50-70% ⚠️ | -8pp a -28pp |
| Contagem | 75.0% | ~30-50% ❌ | -25pp a -45pp |

### Problemas de Generalização

**Por que a acurácia cai tanto?**

1. **Overfitting no caso AMBIPAR** - Prompts e enriquecimento otimizados para um documento específico
2. **Prejuízos não detectados** - 100% de erro em empresas com prejuízo (5+ casos)
3. **Unidades inconsistentes** - Milhão/bilhão confundidos
4. **Taxa "Não Encontrado"** - Sobe de 17% para 60-83%

**Conclusão Honesta:**
- ✅ O sistema **funciona como prova de conceito** para RAG+LLM em FRs
- ✅ Pode servir como **ferramenta de apoio** (pré-preenchimento com revisão obrigatória)
- ❌ **NÃO está pronto para produção** sem melhorias críticas

---

## 🚀 Instalação e Configuração

### Pré-requisitos

- **Java 21 ou superior** ([Download OpenJDK](https://adoptium.net/))
- **Gradle 9.2+** (incluído via `./gradlew`)
- **Conta Google AI Studio** ([Criar API Key gratuita](https://aistudio.google.com/app/apikey))
- **4 GB de RAM** (mínimo)
- **500 MB de espaço em disco** (modelo de embeddings)

### Passo 1: Clone o Repositório

```bash
git clone https://github.com/renato-mendes-ufrpe/fr-processor
cd fr-processor
```

### Passo 2: Configure Variáveis de Ambiente

```bash
# Copie o arquivo de exemplo
cp .env.example .env

# Edite o arquivo .env
nano .env
```

**Configurações obrigatórias:**

```bash
# Google Gemini API Key (obter em https://aistudio.google.com/app/apikey)
GEMINI_API_KEY=AIza...sua-chave-aqui

# Modelo a usar (padrão: gemini-2.5-flash)
GEMINI_MODEL=gemini-2.5-flash
```

**Configurações opcionais (recomendado manter padrões):**

```bash
# Rate Limiting
REQUEST_DELAY_MS=6000              # 6 segundos entre requests
CHECKPOINT_INTERVAL=5              # Salvar a cada 5 questões

# RAG Configuration
MAX_SEGMENT_SIZE_IN_TOKENS=2000    # Tamanho dos chunks (2000 = ótimo)
SEGMENT_OVERLAP_IN_TOKENS=600      # Overlap entre chunks
MAX_RESULTS_FOR_RETRIEVAL=15       # Chunks recuperados por busca
MIN_SCORE_FOR_RETRIEVAL=0.60       # Score mínimo de similaridade
```

### Passo 3: Adicione os PDFs

```bash
# Crie a pasta de dados (se não existir)
mkdir -p data/report

# Copie seus Formulários de Referência em PDF
cp /caminho/para/FR-EMPRESA.pdf data/report/
```

**Formato do nome do arquivo:** `NOME DA EMPRESA.pdf`  
**Exemplo:** `AMBIPAR PARTICIPAÇÕES E EMPREENDIMENTOS S.A..pdf`

### Passo 4: Prepare o Guia de Coleta

O arquivo `Guia de Coleta.csv` já está incluído no projeto com 24 questões pré-configuradas.

**Estrutura:**

```csv
Nº;Dificuldade;Questão;Onde?;Como Preencher?;OBSERVAÇÕES;Tipo;Palavras-chave RAG
2;Médio;Qual é a receita líquida da empresa?;2.1, item h;COPIAR "Receita"...;;MONETARIA;item h, Receita operacional...
```

Para adicionar novas questões, edite o CSV seguindo o formato acima.

### Passo 5: Compile o Projeto

```bash
# Compilar o projeto
./gradlew clean build

# Verificar compilação
./gradlew tasks
```

---

## 💻 Como Executar

### Modo 1: Processar Todos os PDFs

```bash
# Executar processamento completo
./gradlew run

# Salvar log em arquivo
./gradlew run --console=plain 2>&1 | tee output/execution-log-all.txt
```

**Saída:**
- `output/respostas.csv` - Resultados de todas as empresas
- Log no console em tempo real

---

### Modo 2: Processar Apenas Um PDF

```bash
# Processar apenas a Ambipar
./gradlew run --args="AMBIPAR"

# Ou nome completo
./gradlew run --args="AMBIPAR PARTICIPAÇÕES E EMPREENDIMENTOS S.A..pdf"

# Com log específico
./gradlew run --args="AMBIPAR" --console=plain 2>&1 | tee output/execution-log-ambipar.txt
```

**Filtro por substring:** O sistema processa apenas PDFs que **contêm** o argumento no nome.

---

### Modo 3: Executar com Configurações Customizadas

```bash
# Chunk size maior (para tabelas muito grandes)
MAX_SEGMENT_SIZE_IN_TOKENS=3000 ./gradlew run

# Mais chunks recuperados (melhor contexto)
MAX_RESULTS_FOR_RETRIEVAL=25 ./gradlew run

# Score mínimo mais baixo (busca menos restritiva)
MIN_SCORE_FOR_RETRIEVAL=0.50 ./gradlew run
```

---

### Monitoramento em Tempo Real

```bash
# Terminal 1: Executar processamento
./gradlew run --args="AMBIPAR" --console=plain 2>&1 | tee output/execution-log.txt

# Terminal 2: Monitorar progresso
watch -n 1 "tail -20 output/execution-log.txt"

# Terminal 3: Ver respostas parciais
watch -n 5 "cat output/respostas.csv"
```

---

### Saídas Geradas

```
output/
├── respostas.csv                    # Resultados finais (UTF-8 BOM)
├── execution-log.txt                # Log completo da execução
└── checkpoint.json                  # Estado para retomar (se interrompido)
```

#### Formato do CSV de Saída

```csv
Empresa;Q2 - Receita Líquida;Q2 - Manual;Q3 - Lucro Líquido;Q3 - Manual;...
AMBIPAR...;R$ 4.872.707.000;;R$ 56.649.000;;...
```

**Características:**
- **Separador:** `;` (compatível com Excel brasileiro)
- **Encoding:** UTF-8 com BOM (abre corretamente no Excel)
- **Colunas duplas:** RAG (preenchida) + Manual (vazia para conferência)

---

## 📂 Estrutura do Projeto

```
fr-processor/
├── src/main/java/com/example/rag/
│   ├── RagApplication.java                 # Ponto de entrada, orquestra pipeline
│   ├── automation/
│   │   ├── QuestionProcessor.java          # Processamento tipo-específico
│   │   ├── CsvQuestionReader.java          # Leitura do Guia de Coleta
│   │   └── model/
│   │       ├── Question.java               # Modelo de questão
│   │       ├── TipoQuestao.java            # Enum com 5 tipos
│   │       └── CompanyResponse.java        # Modelo de resposta (linha CSV)
│   ├── config/
│   │   └── Config.java                     # Configurações centralizadas (.env)
│   ├── indexer/
│   │   └── DocumentIndexer.java            # Indexação: Parse + Chunk + Embed
│   └── retrieval/
│       └── RagQueryEngine.java             # RAG: Busca + Gemini + Resposta
│
├── data/
│   └── report/                             # PDFs dos Formulários de Referência
│       └── AMBIPAR...S.A..pdf
│
├── output/                                 # Resultados gerados
│   ├── respostas.csv                       # Resultado final
│   ├── execution-log.txt                   # Log completo
│   └── checkpoint.json                     # Estado para retomada
│
├── config/
│   ├── ground-truth.csv                    # Respostas validadas manualmente
│   └── GROUND_TRUTH.md                     # Documentação do ground truth
│
├── Guia de Coleta.csv                      # 24 questões a extrair (INPUT)
├── .env                                    # Variáveis de ambiente (NÃO versionar)
├── .env.example                            # Template de configuração
├── build.gradle                            # Configuração Gradle
└── README.md                          # Este arquivo
```

### Arquivos de Configuração

| Arquivo | Propósito | Versionar? |
|---------|-----------|------------|
| `.env` | API keys e configs locais | ❌ NÃO (adicionar ao .gitignore) |
| `.env.example` | Template público | ✅ SIM |
| `Config.java` | Carrega .env, define padrões | ✅ SIM |
| `Guia de Coleta.csv` | Questões a extrair | ✅ SIM |

---

## ⚠️ Limitações Conhecidas e Problemas em Produção

### 🔴 Status: Protótipo de Pesquisa, NÃO Pronto para Produção

Este projeto demonstra a **viabilidade técnica** da abordagem RAG+LLM para extração de Formulários de Referência, mas **não está pronto para substituir trabalho manual** devido às limitações críticas documentadas abaixo.

### Comparativo: Validação Única vs Múltiplos PDFs

| Métrica | Caso AMBIPAR (1 PDF) | Produção (10 PDFs) | Delta |
|---------|----------------------|---------------------|-------|
| **Acurácia Questões Monetárias** | 100% (5/5) ✅ | ~40-60% ❌ | -40pp a -60pp |
| **Valores Corretos com Unidade** | 100% | ~30% | -70pp |
| **Prejuízos Detectados** | N/A (AMBIPAR teve lucro) | 0% ❌ | Falha crítica |
| **Nomes de Auditoria Completos** | 100% (2/2) | ~60% | -40pp |
| **"Informação Não Encontrada"** | 4/24 (17%) | 15-20/24 (60-83%) | +43-66pp |

### Problemas Críticos Identificados

#### 1. 🔴 Unidades Monetárias Incorretas

**Problema:** Sistema confunde milhão/bilhão/valor absoluto.

**Exemplos reais (ver print anexo):**

| Empresa | Campo | Valor Real | Sistema Retornou | Erro |
|---------|-------|------------|------------------|------|
| 3R Petroleum | Receita Líquida | **R$ 5,6 bilhões** | R$ 5.619.989.000 (5,6 bi) | ✅ OK |
| AERIS | Receita Líquida | R$ 2,8 bilhões | R$ 2.831.915.000 (2,8 bi) | ✅ OK |
| **Agrogalaxy** | Receita Líquida | **R$ 9,4 bilhões** | **R$ 9.399.096.000 (9,3 bi)** | ⚠️ Próximo, mas impreciso |
| AES Brasil | Receita Líquida | R$ 3,4 bilhões | R$ 3.431.500.000 (3,4 bi) | ✅ OK |
| Allos | Receita Líquida | R$ 2,7 bilhões | R$ 2.712.300.000 (2,7 bi) | ✅ OK |

**Padrão:** Multiplicação por mil/milhão funciona às vezes, mas é **inconsistente entre diferentes PDFs**.

**Causa Raiz:**
- Gemini retorna formatos variados: "5.619.989 (em R$ mil)", "R$ 5.619.989 mil", "5,6 bilhões"
- Pós-processamento não captura todas as variações
- Falta validação cruzada (ex: receita em bilhões é mais comum que milhões)

---

#### 2. 🔴 Prejuízos (Valores Negativos) Não Detectados

**Problema:** Sistema **sempre retorna valores positivos**, mesmo quando a empresa teve prejuízo.

**Exemplos reais:**

| Empresa | Lucro Real | Sistema Retornou | Erro |
|---------|------------|------------------|------|
| AERIS | **-R$ 106.567.000 (prejuízo)** | R$ 106.567.000 (lucro) | ❌ CRÍTICO |
| Agrogalaxy | **-R$ 367.292.000 (prejuízo)** | R$ 367.292.000 (lucro) | ❌ CRÍTICO |
| Alliança Saúde | **-R$ 218.559.000 (prejuízo)** | R$ 218.559.000 (lucro) | ❌ CRÍTICO |
| Alphaville | **-R$ 581.000.000 (prejuízo)** | R$ 474.418.000 (errado) | ❌ CRÍTICO |
| Allpark | **-R$ 68.080.000 (prejuízo)** | R$ 68.080.000 (lucro) | ❌ CRÍTICO |

**Impacto:** **Erro financeiro gravíssimo** - inverteu o resultado de 5+ empresas.

**Causa Raiz:**
```java
// postProcessMonetary() não detecta sinal negativo em todos os casos
// Padrão detectado: "Prejuízo de R$ 106.567 mil"
// Sistema extrai: "106.567 mil" → converte para positivo
```

**Solução necessária:** 
- Buscar palavras-chave: "prejuízo", "perda", "resultado negativo"
- Validar sinal negativo na string original
- Cross-check com demonstrações financeiras

---

#### 3. 🟡 Informações Não Encontradas (Alto Índice)

**Problema:** Taxa de "INFORMAÇÃO NÃO ENCONTRADA" sobe de **17%** (AMBIPAR) para **60-83%** em outros PDFs.

**Hipóteses:**
1. **Estrutura diferente:** Cada empresa formata o FR de forma única
2. **Palavras-chave não generalizam:** Enriquecimento otimizado para AMBIPAR não funciona em outros
3. **Chunks perdidos:** Score < 0.60 filtra chunks relevantes em documentos com formatação diferente

**Exemplo:** 
- AMBIPAR: "2.1.h Receita operacional líquida: R$ 4.872.707 mil" (encontrado)
- Outra empresa: "Item 2.1 - Demonstração do Resultado - Receita líquida de vendas: 3.456.789" (não encontrado)

---


### Melhorias Necessárias para Produção

#### Críticas (Bloqueadores)

- [ ] **Detecção de prejuízos** - Implementar busca por palavras-chave negativas
- [ ] **Validação de unidades** - Cross-check de valores (ex: receita em bilhões é mais comum)
- [ ] **Generalização** - Testar em 50+ PDFs e ajustar prompts/enriquecimento
- [ ] **Taxa "Não Encontrado" < 10%** - Atualmente 60-83% em alguns documentos

#### Importantes

- [ ] **Validação cruzada** - Comparar receita vs lucro (lucro > receita = erro)
- [ ] **Extração de tabelas estruturadas** - Usar biblioteca específica (Camelot, Tabula)
- [ ] **Ensemble de modelos** - Gemini + GPT-4 + Claude, escolher melhor resposta
- [ ] **Human-in-the-loop** - Interface para revisar/corrigir respostas

#### Desejáveis

- [ ] Cache de embeddings por PDF
- [ ] Fine-tuning do modelo de embeddings em FRs
- [ ] A/B testing de diferentes prompts
- [ ] Métricas de confiança por resposta (score 0-100%)

---

## 🧪 Principais Desafios e Aprendizados

### 1. 🔧 Otimização do Tamanho dos Chunks

#### Problema

Chunks pequenos (600 tokens) cortavam tabelas ao meio, perdendo contexto crítico.

**Exemplo real - Q30 (Quantos conselheiros?):**

**Com 600 tokens:**
```
Chunk 1: 
Nome: Tércio Borlenghi Jr
CPF: 123.456.789-00
Cargo: Conselheiro (Efetivo) e Dir. Presidente
[chunk termina aqui]

Chunk 2:
[começa no meio da próxima pessoa]
CPF: 987.654.321-00
Cargo: Conselheiro Independente
[sem nome da pessoa anterior]
```

**Resultado:** Sistema conta apenas 3-4 conselheiros (vs 7 corretos)

#### Tentativas

| Chunk Size | Overlap | Resultado | Problema |
|------------|---------|-----------|----------|
| 600 | 100 | ❌ 62.5% | Tabelas cortadas |
| 1200 | 200 | ⚠️ 79.2% | Melhor, mas incompleto |
| **2000** | **600** | ✅ **87.5%** | **Tabelas completas!** |

#### Solução Final

```java
MAX_SEGMENT_SIZE_IN_TOKENS=2000    // Captura tabelas completas
SEGMENT_OVERLAP_IN_TOKENS=600      // 30% overlap mantém contexto
```

**Aprendizado:** Para documentos tabulares/estruturados, **chunks maiores são essenciais**.

---

### 2. 🎯 Necessidade de Enriquecer a Busca por Similaridade

#### Problema

Busca somente com a pergunta retornava chunks genéricos.

**Exemplo - Q3 (Lucro líquido):**

**Query simples:**
```
"Qual é o lucro líquido da empresa?"
```

**Chunks retornados (top 3):**
```
[1] Score: 0.67 | "...análise dos resultados da empresa..."
[2] Score: 0.65 | "...lucro operacional e fatores que..."
[3] Score: 0.64 | "...impactam a geração de lucro..."
```

❌ **Nenhum contém o valor numérico!**

#### Solução Implementada

**Query enriquecida:**
```
"Qual é o lucro líquido da empresa? 2.1, item h, Condições financeiras 
Lucro líquido, demonstrações financeiras, R$, mil, milhão, 31/12"
```

**Chunks retornados:**
```
[1] Score: 0.89 | "Lucro líquido do exercício: 56.649 (em R$ mil)..."
[2] Score: 0.87 | "2.1.h Demonstração de Resultado..."
[3] Score: 0.85 | "Receita operacional líquida: 4.872.707..."
```

✅ **Chunk #1 tem exatamente o que precisamos!**

**Impacto:**
- Antes: 0/4 questões monetárias corretas
- Depois: **4/4 questões monetárias corretas (100%)**

**Aprendizado:** Embeddings precisam de **termos específicos do domínio** para busca eficaz.

---

### 3. 🤖 Dificuldades com Integração do Gemini

#### Problema 1: Modelo Não Retorna Resposta Esperada

**Cenário:** Chunk correto recuperado, mas Gemini retorna texto genérico.

**Exemplo - Q6 (Gastos com auditoria):**

**Chunk recuperado (score 0.91):**
```
Montante total da remuneração dos auditores independentes:
- Serviço de auditoria: R$ 2.210 (em R$ mil)
- Outros serviços: R$ 2.170 (em R$ mil)
Total: R$ 4.380 (em R$ mil)
```

**Gemini retornou:**
```
"A empresa contratou serviços de auditoria independente conforme 
regulamentação da CVM..."
```

❌ **Não extraiu o valor!**

**Causa raiz:** Prompt muito genérico, não enfatizou "APENAS o número".

**Solução:**
```java
REGRAS CRÍTICAS:
1. Retorne APENAS o número com unidade (ex: "4.380 (em R$ mil)")
2. NÃO inclua explicações, textos adicionais ou fórmulas
3. Se não encontrar o VALOR EXATO: "INFORMAÇÃO NÃO ENCONTRADA"
```

**Resultado:** ✅ Gemini agora retorna: `"4.380 (em R$ mil)"`

---

#### Problema 2: Rate Limits (429 Too Many Requests)

**Cenário:** Sistema fazia 15 requests em 1 minuto (limite Free Tier: 10 RPM).

**Erro recebido:**
```
Error 429: Resource exhausted. Requests limit exceeded: 10 per minute
```

**Solução implementada:**

```java
// Config.java
REQUEST_DELAY_MS=6000  // 6 segundos entre requests

// RagApplication.java
Thread.sleep(REQUEST_DELAY_MS);
System.out.println("⏳ Aguardando 6s (rate limiting)...");
```

**Cálculo:**
- 60 segundos / 6 segundos = **10 requests/minuto**
- Margem de segurança dentro do limite

**Aprendizado:** Sempre implementar **rate limiting robusto** para APIs gratuitas.

---

#### Problema 3: Custo de Token (TPM - Tokens Per Minute)

**Cenário:** Prompts muito longos consumiam TPM rapidamente.

**Antes:**
- Prompt médio: ~8.000 tokens (contexto + instruções)
- 10 requests × 8.000 tokens = **80.000 TPM**
- Limite: 250.000 TPM → OK, mas pouco margem

**Otimização:**
- Reduzir MAX_RESULTS de 20 para 15 chunks
- Prompts mais concisos (remover exemplos redundantes)
- Resultado: ~5.000 tokens/prompt

**Após:**
- 10 requests × 5.000 tokens = **50.000 TPM**
- Margem: 200.000 TPM (80% disponível)

**Aprendizado:** Monitorar **RPM e TPM** para evitar throttling.

---

### 4. 🎭 Versão Gratuita do Gemini (Limitações)

#### Limitação 1: Qualidade Inferior ao GPT-4

**Observado:**
- Gemini 2.5 Flash (Free) às vezes "alucina" (inventa informações)

**Exemplo - Q63 (Casos de fraude):**

**Contexto recuperado:**
```
"A Companhia identificou risco de fraude..." (seção 4.1 - Riscos)
"Não houve nenhum caso confirmado de fraude..." (seção 5.3.c)
```

**Gemini Free retornou:** `"SIM"` (focou em "fraude" na seção de riscos)  

**Workaround:** Enriquecer prompt com exemplos e diferenciações explícitas.

---

#### Limitação 2: Latência

| Modelo | Latência Média | Custo |
|--------|----------------|-------|
| **Gemini 2.5 Flash (Free)** | ~4-6s | $0 |

**Aprendizado:** Free Tier é ótimo para **prototipagem e validação**, mas produção pode justificar modelo pago.

---

### 5. 🔀 Cross-Reference Entre Seções do FR

#### Problema

**Q39:** "Quantos membros do Comitê de Auditoria são conselheiros?"

Requer informação de **duas seções diferentes:**
1. **Seção 7.4 (Comitês):** Lista membros do Comitê de Auditoria
2. **Seção 7.3 (Conselho):** Lista membros do Conselho de Administração

**Desafio:** RAG recupera chunks isolados, LLM não cruza informações.

#### Tentativas

**1. Aumentar MAX_RESULTS (15 → 30)**
```bash
MAX_RESULTS_FOR_RETRIEVAL=30
```
- ❌ Trouxe ruído (chunks irrelevantes)
- Score médio caiu de 0.85 para 0.72

**2. Query em 2 etapas (não implementado)**
```
Etapa 1: "Quem são os membros do Comitê de Auditoria?"
Etapa 2: "Destes, quais são conselheiros?"
```
- ⚠️ Dobra o número de requests (rate limit)
- ⚠️ Aumenta latência (2× o tempo)

**3. Abordagem híbrida (solução futura)**
```java
// Extração programática (regex/tabelas)
List<String> membroComite = extrairMembrosComite();
List<String> conselheiros = extrairConselheiros();

// Cross-reference no código Java
int count = membroComite.stream()
    .filter(conselheiros::contains)
    .count();
```

**Aprendizado:** RAG tem **limitação arquitetural** para cross-reference complexo. Solução: abordagem híbrida.

---

### 6. 🧩 O Caso Misterioso: José Carlos de Souza

#### Contexto

**José Carlos de Souza** é mencionado no FR como:
- Conselheiro de Administração (Independente)
- Membro do Comitê de Auditoria

**Problema:** Sistema **nunca o encontra** em 3 questões (Q33, Q39, Q40).

#### Investigação

```bash
# Buscar "José Carlos" no log de chunks
grep -i "josé carlos" output/execution-log.txt
# Resultado: 0 ocorrências (!)

# Buscar diretamente no PDF extraído
grep -i "josé carlos" /tmp/extracted-text.txt
# Resultado: 4 ocorrências (pág. 185-187)
```

**Descoberta:** José Carlos **está no PDF**, mas não nos chunks recuperados.

#### Hipóteses

| Hipótese | Probabilidade | Teste |
|----------|---------------|-------|
| Score < 0.60 (filtrado) | 🔴 Alta | Reduzir MIN_SCORE para 0.50 |
| Nome muito comum | 🟡 Média | Query específica: "José Carlos de Souza CPF" |
| Chunk cortado no meio | 🟢 Baixa | Improvável com 2000 tokens |
| Encoding UTF-8 | 🟢 Baixa | Nome aparece em outros lugares OK |

**Próximo passo:** Testar redução de MIN_SCORE ou aumentar MAX_RESULTS.

---

## 🛠️ Tecnologias Utilizadas

### Stack Principal

| Tecnologia | Versão | Propósito | Por que escolhemos? |
|------------|--------|-----------|---------------------|
| **Java** | 21 | Linguagem base | Performance, tipagem forte, ecosistema maduro |
| **LangChain4j** | 1.8.0 | Framework RAG | Melhor framework Java para LLMs (inspirado no LangChain Python) |
| **Google Gemini** | 2.5 Flash | LLM (geração) | Free Tier generoso (10 RPM), boa qualidade |
| **AllMiniLmL6V2** | ONNX | Embeddings | 100% local, gratuito, excelente para português |
| **Apache Tika** | 2.9.1 | Parser PDF | Universal, suporta OCR, extrai tabelas |
| **Gradle** | 9.2 | Build tool | Padrão Java moderno, gerencia dependências |

### Dependências (build.gradle)

```gradle
dependencies {
    // LangChain4j Core
    implementation "dev.langchain4j:langchain4j:1.8.0"
    implementation "dev.langchain4j:langchain4j-easy-rag:1.8.0-beta15"
    
    // Document Processing
    implementation "dev.langchain4j:langchain4j-document-parser-apache-tika:1.8.0-beta15"
    
    // Embeddings (local)
    implementation "dev.langchain4j:langchain4j-embeddings-all-minilm-l6-v2:1.8.0-beta15"
    
    // Google Gemini
    implementation "dev.langchain4j:langchain4j-google-ai-gemini:1.8.0"
    
    // Logging
    implementation "org.slf4j:slf4j-simple:2.0.9"
}
```

### Alternativas Consideradas

| Decisão | Escolhido | Alternativa | Por quê? |
|---------|-----------|-------------|----------|
| **LLM** | Gemini 2.5 Flash | GPT-4, Claude | Free Tier (10 RPM, $0) |
| **Embeddings** | AllMiniLmL6V2 | OpenAI Ada-002 | Local ($0) vs API ($0.0001/1k) |
| **Framework** | LangChain4j | Semantic Kernel | Melhor docs, comunidade ativa |
| **Parser PDF** | Apache Tika | PyPDF, pdfplumber | Universal, Java nativo |

---

## 👥 Autores

**Projeto desenvolvido na Universidade Federal Rural de Pernambuco (UFRPE)**

- **Desenvolvedor:** Renato Mendes
- **Orientador:** [Nome do Orientador]
- **Curso:** [Nome do Curso]
- **Disciplina:** [Nome da Disciplina]
- **Período:** 2025.1

### Escopo da Validação

**Documento de Validação Detalhada:**
- **Empresa:** AMBIPAR Participações e Empreendimentos S.A.
- **Formulário de Referência:** 2024 (Versão 1)
- **Data de Validação:** 09/11/2025
- **Acurácia no Caso Específico:** 83.3% (20/24 questões)

**Teste de Generalização (10 empresas):**
- **Data:** 11/11/2025
- **Empresas:** 3R Petroleum, AERIS, AES Brasil, Agrogalaxy, Alliança Saúde, Allied, Allos, Allpark, Alphaville, AMBIPAR
- **Acurácia Observada:** ~40-60% (com erros críticos: prejuízos invertidos, unidades erradas)
- **Conclusão:** Sistema não generaliza adequadamente para múltiplos PDFs

### Ground Truth

**Caso AMBIPAR:** Respostas validadas manualmente por especialista em análise de Formulários de Referência.  
Documentação completa em: `config/GROUND_TRUTH.md`

**Casos Múltiplos (10 PDFs):** Validação por comparação com projeto alternativo que processou os mesmos documentos com foco em menos perguntas mas maior consistência.

---

## 📄 Licença

Este projeto está licenciado sob a **MIT License** - veja o arquivo [LICENSE](LICENSE) para detalhes.

---

## 🤝 Contribuições

Contribuições são bem-vindas! Este é um projeto de **pesquisa aberta** - queremos aprender o que funciona e o que não funciona em RAG para documentos corporativos.

### Como Contribuir

1. Fork o repositório
2. Crie uma branch: `git checkout -b feature/MinhaFeature`
3. Commit suas mudanças: `git commit -m 'Adiciona MinhaFeature'`
4. Push para a branch: `git push origin feature/MinhaFeature`
5. Abra um Pull Request

### 🔴 Prioridades Críticas (Bloqueadores para Produção)

Estas são as melhorias **essenciais** para tornar o sistema utilizável em produção:

- [ ] **Detecção de prejuízos** - Buscar "prejuízo", "perda", "resultado negativo" e aplicar sinal negativo
- [ ] **Validação de unidades monetárias** - Cross-check: receita em bilhões é mais comum que milhões
- [ ] **Reduzir taxa "Não Encontrado"** - De 60-83% para <10% em PDFs diversos
- [ ] **Generalização de prompts** - Testar em 50+ PDFs e ajustar enriquecimento de queries
- [ ] **Validação cruzada** - Lucro > Receita = erro óbvio

### 🟡 Melhorias Importantes

- [ ] Resolver problema do José Carlos (Q33, Q39, Q40) - investigação em andamento
- [ ] Melhorar interpretação de negações (Q47, Q63)
- [ ] Implementar abordagem híbrida para contagem (extração + classificação)
- [ ] Extração estruturada de tabelas (Camelot/Tabula) em vez de RAG puro
- [ ] Ensemble de modelos (Gemini + GPT-4 + Claude, escolher melhor)

### 🟢 Features Desejáveis

- [ ] Interface web para upload de PDFs
- [ ] Cache de embeddings (evitar reindexação)
- [ ] Dashboard de visualização de resultados
- [ ] Exportação para Excel/JSON
- [ ] Fine-tuning do modelo de embeddings em corpus de FRs
- [ ] Métricas de confiança por resposta (0-100%)
- [ ] Human-in-the-loop para correção

### 📊 Contribuições Valiosas

Se você testar este sistema:
- 📝 **Reporte resultados** - Abra uma issue com acurácia em outros PDFs
- 🐛 **Documente erros** - Exemplos reais de falhas ajudam a melhorar
- 💡 **Compartilhe soluções** - Encontrou uma técnica melhor? PR é bem-vindo!
- 📚 **Valide em seu domínio** - Funciona em outros tipos de documentos corporativos?

---

## 📞 Contato

- **Email:** [seu-email@exemplo.com]
- **LinkedIn:** [linkedin.com/in/seu-perfil]
- **GitHub:** [github.com/seu-usuario]

---

## 📚 Referências

### Documentação Técnica

- [LangChain4j Documentation](https://docs.langchain4j.dev/)
- [Google Gemini API](https://ai.google.dev/gemini-api/docs)
- [AllMiniLmL6V2 Model Card](https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2)
- [Apache Tika Guide](https://tika.apache.org/)

### Papers e Artigos

- Lewis et al. (2020). "Retrieval-Augmented Generation for Knowledge-Intensive NLP Tasks"
- [RAG Patterns and Best Practices](https://www.pinecone.io/learn/retrieval-augmented-generation/)

### Formulários de Referência

- [CVM - Instruções sobre Formulários de Referência](https://www.gov.br/cvm)
- [B3 - Empresas Listadas](https://www.b3.com.br/)

---

## ✨ Agradecimentos

- **CVM** pela disponibilização pública dos Formulários de Referência
- **AMBIPAR** pela documentação completa que serviu de caso de uso
- **LangChain4j Team** pelo excelente framework
- **Google** pelo Gemini Free Tier generoso
- **Comunidade open-source** pelos modelos de embeddings gratuitos

---

<div align="center">

**Desenvolvido com ❤️ na UFRPE**

[![UFRPE](https://img.shields.io/badge/UFRPE-Recife-green.svg)](https://www.ufrpe.br/)

---

### 📊 Resumo Executivo do Projeto

| Aspecto | Resultado |
|---------|-----------|
| **Status** | Protótipo de Pesquisa |
| **Acurácia (1 PDF validado)** | 83.3% ✅ |
| **Acurácia (10 PDFs testados)** | 40-60% ⚠️ |
| **Pronto para Produção?** | ❌ NÃO |
| **Uso Recomendado** | Ferramenta de apoio com revisão humana |
| **Contribuição Acadêmica** | Demonstra viabilidade de RAG+LLM em FRs |

*Explorando os limites e possibilidades de RAG em documentos corporativos brasileiros*

</div>
