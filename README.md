# 📊 Processamento Automatizado de Formulários de Referência

Sistema inteligente para extração automatizada de informações de Formulários de Referência (FR) corporativos, utilizando **RAG (Retrieval-Augmented Generation)** com LangChain4j e Google Gemini.

## 🎯 O que este projeto faz?

Este sistema processa Formulários de Referência em PDF e extrai automaticamente informações financeiras e corporativas específicas, seguindo um guia de coleta predefinido. As respostas são salvas em formato CSV, prontas para análise.

**Questões automatizadas:**
- ✅ Receita líquida da empresa
- ✅ Lucro líquido da empresa  
- ✅ Firma de auditoria independente
- ✅ Gastos anuais com auditoria
- ✅ Gastos com serviços adicionais de auditoria

## 🚀 Como Funciona?

### Arquitetura RAG

```
📄 Formulário de Referência (PDF)
          ↓
    [1. INDEXAÇÃO]
    - Extração de texto (Apache Tika)
    - Divisão em chunks (1200 tokens)
    - Geração de embeddings (AllMiniLmL6V2)
    - Armazenamento vetorial em memória
          ↓
📋 Guia de Coleta.csv → [2. PROCESSAMENTO]
    Para cada questão:
    - Enriquecimento da query com termos do guia
    - Busca semântica (similaridade de cosseno)
    - Recuperação dos top 10 chunks relevantes
    - Construção de prompt estruturado
    - Geração de resposta (Google Gemini)
    - Pós-processamento (formatação monetária)
          ↓
    📊 output/respostas.csv
```

### Tecnologias Utilizadas

- **[LangChain4j 1.8.0](https://github.com/langchain4j/langchain4j)** - Framework Java para LLMs
- **[Google Gemini 2.5 Flash](https://ai.google.dev/)** - Modelo de geração de respostas
- **[AllMiniLmL6V2](https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2)** - Modelo local de embeddings (384 dimensões)
- **[Apache Tika](https://tika.apache.org/)** - Extração de texto de PDFs
- **Java 21** - Linguagem de programação
- **Gradle 9.2** - Gerenciamento de build e dependências

## 📦 Instalação

### Pré-requisitos

- **Java 21+** (recomendado: SDKMAN)
  ```bash
  sdk install java 21.0.7-tem
  sdk use java 21.0.7-tem
  ```

- **Gradle 9.2+** (incluído via wrapper)
  ```bash
  ./gradlew --version
  ```

### Configuração

1. **Clone o repositório:**
   ```bash
   git clone <seu-repositorio>
   cd rag-java-example
   ```

2. **Configure a API do Google Gemini:**
   
   Crie um arquivo `.env` na raiz do projeto:
   ```env
   GEMINI_API_KEY=sua-chave-aqui
   GEMINI_MODEL=gemini-2.5-flash
   ```
   
   Obtenha sua chave em: https://aistudio.google.com/app/apikey

3. **Adicione seus arquivos:**
   
   - Coloque o PDF do Formulário de Referência em `data/report/`
   - Certifique-se que `Guia de Coleta.csv` está na raiz do projeto

4. **Ajuste configurações (opcional):**
   
   Edite `src/main/java/com/example/rag/config/Config.java`:
   ```java
   public static final String AMBIPAR_PDF_FILE = "SEU-ARQUIVO.pdf";
   ```

## 🎮 Uso

### Executar o processamento

```bash
./gradlew run
```

O sistema irá:
1. Indexar o PDF (primeira vez pode demorar ~10s)
2. Processar as 5 questões automaticamente
3. Gerar `output/respostas.csv` com as respostas

### Exemplo de saída

```csv
Nome_Empresa;Resposta_02;Resposta_03;Resposta_05;Resposta_06;Resposta_08
Ambipar S.A.;R$ 4.872.707.000;R$ 56.649.000;BDO RCS Auditores Independentes SS Ltda.;R$ 4.380.131;R$ 2.170.130
```

### Compilar sem executar

```bash
./gradlew build -x test
```

### Limpar build anterior

```bash
./gradlew clean
```

## 📁 Estrutura do Projeto

```
rag-java-example/
├── src/main/java/com/example/rag/
│   ├── RagApplication.java              # Aplicação principal
│   ├── config/
│   │   └── Config.java                  # Configurações (chunking, retrieval, etc)
│   ├── indexer/
│   │   └── DocumentIndexer.java         # Indexação de PDFs
│   ├── retrieval/
│   │   └── RagQueryEngine.java          # Motor RAG (busca + geração)
│   └── automation/
│       ├── CsvQuestionReader.java       # Leitor do guia CSV
│       ├── QuestionProcessor.java       # Processador de questões
│       └── model/
│           ├── Question.java            # Modelo de questão
│           └── CompanyResponse.java     # Modelo de resposta
├── data/report/                         # PDFs de entrada
├── output/                              # CSVs de saída
├── Guia de Coleta.csv                   # Questões a processar
├── .env                                 # Credenciais (não commitado)
└── build.gradle                         # Dependências
```

## ⚙️ Configurações Avançadas

### Parâmetros de Chunking

Em `Config.java`:

```java
// Tamanho de cada chunk (ajuste conforme complexidade do documento)
public static final int MAX_SEGMENT_SIZE_IN_TOKENS = 1200;

// Overlap entre chunks (previne perda de contexto)
public static final int SEGMENT_OVERLAP_IN_TOKENS = 200;
```

### Parâmetros de Retrieval

```java
// Número de chunks recuperados para cada questão
public static final int MAX_RESULTS_FOR_RETRIEVAL = 10;

// Score mínimo de similaridade (0.0 a 1.0)
public static final double MIN_SCORE_FOR_RETRIEVAL = 0.65;
```

### Customizar Questões

Edite `Guia de Coleta.csv` com o formato:

```csv
Nº;Dificuldade;Questão;Onde?;Como Preencher?;OBSERVAÇÕES
2;Médio;Qual é a receita líquida?;FR - 2.1.h;COPIAR "Receita";Campo aberto
```

## 🧪 Performance

| Métrica | Valor |
|---------|-------|
| **Indexação** | ~10s para 200 páginas |
| **Processamento/questão** | ~8s (RAG + Gemini) |
| **Total (5 questões)** | ~50s |
| **Chunks gerados** | ~763 (doc 200 pág) |
| **Tamanho do chunk** | ~900 palavras |

## 🤔 Como o Sistema é Otimizado?

### 1. **Query Enrichment**
Antes de buscar, o sistema enriquece a query com termos do guia:

```
Query original: "Qual é a receita líquida da empresa?"
Query enriquecida: "Qual é a receita líquida da empresa? FR 2.1.h Condições financeiras 
                    Receita líquida operacional demonstração resultado R$ mil milhão..."
```

### 2. **Chunks Maiores**
Chunks de 1200 tokens capturam tabelas completas e contexto adequado:
- ✅ Tabelas não são fragmentadas
- ✅ Valores numéricos ficam com suas descrições
- ✅ Seções mantêm título + conteúdo juntos

### 3. **Pós-Processamento Inteligente**
O sistema aplica regras automaticamente:
- Multiplicação por 1.000 ou 1.000.000 (quando valor está em R$ mil)
- Formatação monetária brasileira (R$ 1.234.567)
- Limpeza de texto desnecessário

## ❓ FAQ

**P: Preciso de internet para rodar?**  
R: Sim, mas apenas na primeira execução (download do modelo de embeddings ~80MB). Após isso, o modelo fica em cache local. O Gemini sempre requer internet.

**P: Posso usar outros modelos de LLM?**  
R: Sim! O LangChain4j suporta OpenAI, Ollama, Azure OpenAI, etc. Basta ajustar a inicialização em `RagQueryEngine.java`.

**P: Como adicionar mais questões?**  
R: Adicione novas linhas no `Guia de Coleta.csv` e ajuste `CompanyResponse.java` para incluir as novas colunas de resposta.

**P: O sistema funciona com outros tipos de documentos?**  
R: Sim! Qualquer PDF pode ser processado. Ajuste o `AMBIPAR_PDF_FILE` em `Config.java` e adapte as questões no CSV.

**P: Por que RAG ao invés de perguntar direto ao LLM?**  
R: RAG garante que as respostas sejam baseadas **exclusivamente** no documento fornecido, evitando "alucinações" do LLM. É essencial para informações factuais e regulatórias.

## 📝 Licença

Este projeto é fornecido como está, sem garantias. Use por sua conta e risco.

## 🤝 Contribuições

Melhorias e sugestões são bem-vindas! Abra uma issue ou pull request.

---

**Desenvolvido com ☕ e 🤖 por Renato Mendes**
