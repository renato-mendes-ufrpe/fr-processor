package com.example.rag.automation.model;

/**
 * Tipos de questões do Formulário de Referência.
 * 
 * Define categorias para aplicar estratégias específicas de:
 * - Enriquecimento de query
 * - Construção de prompt
 * - Pós-processamento de resposta
 */
public enum TipoQuestao {
    
    /**
     * Questões que exigem valores monetários.
     * Ex: Q2 (receita líquida), Q3 (lucro líquido), Q6 (gastos auditoria)
     * 
     * Características:
     * - Resposta esperada: R$ x.xxx.xxx,xx
     * - Pode estar em R$ mil ou R$ milhão (requer multiplicação)
     * - Geralmente em tabelas de DFs ou seção 2.1.h
     */
    MONETARIA,
    
    /**
     * Questões com respostas SIM/NÃO/NÃO DIVULGADO/NÃO APLICADO.
     * Ex: Q10 (possui política riscos?), Q18 (divulga ASG?), Q41 (coordenador independente?)
     * 
     * Características:
     * - Resposta esperada: apenas "SIM", "NÃO", "NÃO DIVULGADO" ou "NÃO APLICADO"
     * - NUNCA incluir texto explicativo (ex: "SIM = a empresa cita...")
     * - Baseia-se em afirmações do documento
     */
    SIM_NAO,
    
    /**
     * Questões que exigem contar elementos (membros, comitês, etc).
     * Ex: Q23 (quantos comitês?), Q30 (quantos conselheiros?), Q31 (quantas mulheres?)
     * 
     * Características:
     * - Resposta esperada: número inteiro (0, 1, 2, 3...)
     * - Geralmente requer identificar tabelas/listas em seções 7.3, 7.4
     * - Atenção a regras: efetivos vs suplentes, titulares vs temporários
     */
    CONTAGEM,
    
    /**
     * Questões que exigem extrair texto específico do documento.
     * Ex: Q5 (nome da firma auditoria), Q27 (nome da política de conflitos)
     * 
     * Características:
     * - Resposta esperada: texto curto e específico
     * - Copiar exatamente como está no documento
     * - Limpar formatação desnecessária mas preservar conteúdo
     */
    TEXTO_ESPECIFICO,
    
    /**
     * Questões com múltiplas opções pré-definidas.
     * Ex: Q47 (tipo de seguro: "Seguro D&O" | "Outra forma" | "Não" | "Não Divulgado")
     * 
     * Características:
     * - Resposta esperada: uma das opções listadas em "Como Preencher?"
     * - Pode ter 3, 4 ou mais opções
     * - Sistema deve escolher a opção correta baseada no documento
     */
    MULTIPLA_ESCOLHA;
    
    /**
     * Verifica se a questão é do tipo que aceita "NÃO DIVULGADO" ou "NÃO APLICADO".
     */
    public boolean aceitaNaoDivulgado() {
        return this == SIM_NAO || this == MULTIPLA_ESCOLHA;
    }
    
    /**
     * Verifica se a questão exige resposta numérica.
     */
    public boolean isNumerica() {
        return this == MONETARIA || this == CONTAGEM;
    }
    
    /**
     * Verifica se a questão exige resposta exata (sem variações).
     */
    public boolean exigeRespostaExata() {
        return this == SIM_NAO || this == MULTIPLA_ESCOLHA || this == CONTAGEM;
    }
}
