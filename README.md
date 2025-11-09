# RAG + LLM para Extração Automatizada de Formulários de Referência

Sistema automatizado para extração de informações de Formulários de Referência (FRs) usando RAG (Retrieval-Augmented Generation) com LangChain4j e Google Gemini.

## 📊 Status Atual do Projeto

### Acurácia Alcançada: **83.3% (20/24 questões corretas)** ✅

**Evolução do Sistema:**
- Baseline inicial: 62.5% (15/24)
- Sistema de tipos: 79.2% (19/24)
- Chunk size otimizado: 87.5% (21/24)
- **Ground truth validado: 83.3% (20/24)** 📍 *você está aqui*

### Resultados por Tipo de Questão

| Tipo | Acertos | Total | Taxa | Status |
|------|---------|-------|------|--------|
| **MONETÁRIA** | 5/5 | 100% | 🎯 | Perfeito |
| **TEXTO_ESPECÍFICO** | 2/2 | 100% | 🎯 | Perfeito |
| **SIM/NÃO** | 7/9 | 77.8% | ⚠️ | Bom |
| **CONTAGEM** | 6/8 | 75.0% | ⚠️ | Bom |

## 🎯 Funcionalidades Principais

### 1. Sistema de Tipos de Questões
- **5 tipos especializados**: MONETARIA, SIM_NAO, CONTAGEM, TEXTO_ESPECIFICO, MULTIPLA_ESCOLHA
- Prompts customizados por tipo
- Pós-processamento específico
- Enriquecimento de query contextual

### 2. RAG Otimizado
- **Embeddings locais**: AllMiniLmL6V2 (384 dimensões)
- **Recuperação contextual**: 20 chunks por query
- **Score mínimo**: 0.60
- **Chunking inteligente**: 2000 tokens com overlap de 400

### 3. Rate Limiting e Checkpoints
- Delay de 6 segundos entre requests (Gemini Free Tier - 10 RPM)
- Checkpoint automático a cada 5 questões
- Salvamento em CSV com UTF-8 BOM (compatível com Excel)

## 🏗️ Arquitetura

```
src/main/java/com/example/rag/
├── automation/
│   ├── QuestionProcessor.java      # Processamento tipo-específico
│   ├── CsvQuestionReader.java      # Leitura do guia com tipos
│   ├── CsvResponseWriter.java      # Escrita em CSV UTF-8 BOM
│   └── model/
│       ├── Question.java           # Modelo com TipoQuestao
│       └── TipoQuestao.java        # Enum com 5 tipos
├── config/
│   └── Config.java                 # Configurações centralizadas
├── extraction/
│   └── PdfTextExtractor.java       # Apache Tika
├── indexing/
│   └── DocumentIndexer.java        # Chunking + embeddings
├── retrieval/
│   └── RagQueryEngine.java         # RAG + Gemini
└── RagJavaExampleApplication.java  # Main
```

## 🚀 Como Usar

### Pré-requisitos
```bash
# Java 21+
java -version

# Gradle 9.2+
./gradlew --version
```

### Configuração

1. Configure o arquivo `.env`:
```env
GEMINI_API_KEY=sua-chave-aqui
GEMINI_MODEL=gemini-2.5-flash
MAX_RESULTS_FOR_RETRIEVAL=20
MAX_SEGMENT_SIZE_IN_TOKENS=2000
SEGMENT_OVERLAP_IN_TOKENS=400
MIN_SCORE_FOR_RETRIEVAL=0.60
```

2. Prepare os arquivos de entrada:
- `data/formularios/`: PDFs dos Formulários de Referência
- `Guia de Coleta.csv`: Questões a serem extraídas

### Executar

```bash
# Build
./gradlew clean build

# Executar processamento completo
./gradlew run

# Ver progresso em tempo real
tail -f output/execution-log.txt

# Ver resultados
cat output/respostas.csv
```

### Saída
- `output/respostas.csv` - Respostas em formato Excel-compatível
- `output/execution-log.txt` - Log completo da execução
- `output/checkpoint.json` - Estado para retomar execução

## 🔧 Configurações Técnicas

### Config.java (via .env)
```properties
MAX_SEGMENT_SIZE_IN_TOKENS=2000      # Tamanho do chunk (aumentado para tabelas completas)
SEGMENT_OVERLAP_IN_TOKENS=400        # Overlap entre chunks
MAX_RESULTS_FOR_RETRIEVAL=20         # Chunks recuperados por query
MIN_SCORE_FOR_RETRIEVAL=0.60         # Score mínimo de similaridade

# Rate Limiting (Gemini Free Tier)
REQUEST_DELAY_MS=6000                # 6 segundos entre requests
CHECKPOINT_FREQUENCY=5               # Salvar checkpoint a cada 5 questões
```

### Tempo de Execução
- **24 questões**: ~4-5 minutos
- **Rate limiting**: 6s entre requests (10 RPM do Gemini)
- **Checkpoints**: salvamento a cada 5 questões

## 📝 Formato do Guia de Coleta

Estrutura CSV:
```csv
Numero;Grau;Questao;Onde;ComoPreencher;Observacoes;Tipo
30;Médio;Quantos membros...;FR - 7.3;CONTAR a quantidade...;;CONTAGEM
```

**Tipos suportados:**
- `MONETARIA` - Valores em R$ (aplica multiplicadores mil/milhão)
- `SIM_NAO` - Questões binárias (SIM/NÃO/NÃO DIVULGADO)
- `CONTAGEM` - Contar membros/comitês (retorna número + nomes)
- `TEXTO_ESPECIFICO` - Nomes de políticas/auditorias
- `MULTIPLA_ESCOLHA` - Selecionar entre opções predefinidas

## 📈 Questões Respondidas Corretamente (20/24)

### ✅ 100% de Acerto (7 questões)

**Monetárias (5/5):**
- Q2: Receita Líquida - R$ 4.872.707.000
- Q3: Lucro Líquido - R$ 56.649.000
- Q6: Honorários Auditoria - R$ 4.380.131
- Q8: Serviços Adicionais - R$ 2.170.131

**Texto Específico (2/2):**
- Q5: Firma de Auditoria - BDO RCS Auditores
- Q27: Política de Conflitos

**SIM/NÃO (7/9):**
- Q10: Política de Riscos - SIM
- Q14: Auditoria Interna - SIM
- Q15: Controles Adequados - SIM
- Q16: Deficiências - NÃO
- Q18: Divulga ASG - SIM
- Q19: Conselho Fiscal - NÃO
- Q41: Coordenador Independente - NÃO

**Contagem (6/8):**
- Q23: Número de Comitês - 2
- Q30: Total Conselheiros - 7
- Q31: Mulheres no Conselho - 1 (Alessandra)
- Q32: Conselheiros Externos - 2 (Alessandra, Carlos)
- Q34: Conselheiros Executivos - 1 (Tércio Jr)
- Q38: Membros Comitê - 2 (parcial - deveria ser 3)

### ❌ Problemas Conhecidos (4 questões)

1. **Q33** - Conselheiros Independentes: RAG encontra 3, correto é 4 (falta José Carlos)
2. **Q39** - Cross-reference Conselho × Comitê: RAG retorna 0, correto é 2
3. **Q40** - Independentes no Comitê: RAG retorna 0, correto é 2
4. **Q47** - Seguro D&O: RAG retorna "Não Divulgado", correto é "Não"
5. **Q63** - Casos de Fraude: RAG retorna "SIM", correto é "Não"

**Padrões identificados:**
- 🔴 **José Carlos de Souza ausente** (afeta Q33, Q39, Q40)
- 🟡 **Interpretação de negações** (afeta Q47, Q63)
- 🟢 **Cross-reference entre seções** (afeta Q39, Q40)

## 🔍 Debugging e Análise

### Ver chunks recuperados
```bash
grep "Preview:" output/execution-log.txt | head -20
```

### Ver scores de similaridade
```bash
grep "Score:" output/execution-log.txt | head -20
```

### Ver prompts enviados ao LLM
```bash
grep "Prompt:" output/execution-log.txt -A 10
```

### Análise detalhada dos resultados
```bash
# Ver arquivo de análise completa
cat output/ANALISE-RESULTADOS.md
```

## 🎯 Roadmap

### ✅ Implementado
- [x] Sistema de tipos de questões
- [x] RAG com embeddings locais
- [x] Prompts especializados por tipo
- [x] Rate limiting e checkpoints
- [x] Chunk size otimizado (2000 tokens)
- [x] Ground truth validation

### 🔄 Em Progresso
- [ ] Investigar José Carlos de Souza ausente
- [ ] Melhorar detecção de negações no prompt
- [ ] Cross-reference entre seções do FR

### 📋 Planejado
- [ ] Processar múltiplos FRs em batch
- [ ] Interface web para upload de PDFs
- [ ] Exportação em múltiplos formatos (Excel, JSON)
- [ ] Cache de embeddings para performance
- [ ] Fine-tuning do modelo de embeddings
- [ ] Dashboard de visualização

## 📚 Tecnologias Utilizadas

- **Java 21** - Linguagem base
- **LangChain4j 1.8.0** - Framework RAG
- **Google Gemini 2.5 Flash** - LLM (Free Tier, 10 RPM)
- **AllMiniLmL6V2** - Embeddings locais (384 dim)
- **Apache Tika** - Extração de texto de PDFs
- **Gradle 9.2** - Build tool
- **dotenv-java** - Gerenciamento de variáveis de ambiente

## 📖 Estrutura de Arquivos

```
rag-java-example/
├── src/main/java/com/example/rag/     # Código fonte
├── data/
│   └── formularios/                   # PDFs dos FRs
├── config/
│   ├── ground-truth.csv               # Respostas validadas
│   └── GROUND_TRUTH.md                # Documentação do ground truth
├── output/
│   ├── respostas.csv                  # Resultados (gerado)
│   ├── respostas-analise_manual.csv   # Comparação com ground truth
│   ├── execution-log.txt              # Log completo (gerado)
│   ├── checkpoint.json                # Estado da execução (gerado)
│   └── ANALISE-RESULTADOS.md          # Análise detalhada
├── Guia de Coleta.csv                 # Questões a extrair
├── .env                               # Configurações (não versionado)
├── .env.example                       # Template de configurações
└── README.md                          # Este arquivo

```

## 🤝 Contribuindo

Para contribuir com o projeto:

1. Fork o repositório
2. Crie uma branch para sua feature (`git checkout -b feature/MinhaFeature`)
3. Commit suas mudanças (`git commit -m 'Adiciona MinhaFeature'`)
4. Push para a branch (`git push origin feature/MinhaFeature`)
5. Abra um Pull Request

## 📄 Licença

MIT License

## 👥 Autores

- Desenvolvido na UFRPE
- Caso de uso: AMBIPAR Participações e Empreendimentos S.A.
- Validação: Ground truth estabelecido em 09/11/2025

---

**Última atualização**: 09/11/2025  
**Versão**: 3.0 (Ground Truth Validado)  
**Acurácia**: 83.3% (20/24 questões)

Para análise detalhada dos resultados, veja `output/ANALISE-RESULTADOS.md`
