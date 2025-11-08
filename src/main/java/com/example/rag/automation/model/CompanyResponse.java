package com.example.rag.automation.model;

/**
 * Representa a resposta de uma empresa para as questões do guia.
 * 
 * Formato do CSV de saída:
 * Nome_Empresa;Resposta_02;Resposta_03;Resposta_05;Resposta_06;Resposta_08;Resposta_10;Resposta_14;Resposta_15;Resposta_16;Resposta_18;
 * Resposta_19;Resposta_23;Resposta_27;Resposta_30;Resposta_31;Resposta_32;Resposta_33;Resposta_34;Resposta_38;Resposta_39;
 * Resposta_40;Resposta_41;Resposta_47;Resposta_63
 * 
 * Questões correspondentes:
 * - Resposta_02: Qual é a receita líquida da empresa?
 * - Resposta_03: Qual é o lucro líquido da empresa?
 * - Resposta_05: Qual é a firma de auditoria independente?
 * - Resposta_06: Qual o valor dos gastos anuais com a empresa de auditoria independente?
 * - Resposta_08: Qual o valor dos eventuais gastos com serviços adicionais prestados pela firma de auditoria independente?
 * - Resposta_10: A companhia possui uma política de gerenciamento de riscos?
 * - Resposta_14: A empresa divulga a existência de auditoria interna?
 * - Resposta_15: O sistema de controles internos da companhia está adequado?
 * - Resposta_16: Houve deficiências ou recomendações significativas sobre os controles internos apresentadas pelo auditor independente?
 * - Resposta_18: O emissor divulga informações sociais e ambientais e de governança corporativa (ASG)?
 * - Resposta_19: O Conselho Fiscal está instalado?
 * - Resposta_23: Quantos Comitês do Conselho de Administração?
 * - Resposta_27: A companhia possui regras específicas de identificação e administração de conflitos de interesses?
 * - Resposta_30: Quantos membros compõem o Conselho de Administração?
 * - Resposta_31: Quantas mulheres no Conselho de Administração?
 * - Resposta_32: Quantos conselheiros são externos?
 * - Resposta_33: Quantos conselheiros são independentes?
 * - Resposta_34: Quantos conselheiros são executivos da Empresa?
 * - Resposta_38: Quantos membros compõem o Comitê de Auditoria?
 * - Resposta_39: Quantos membros do Comitê de Auditoria são conselheiros?
 * - Resposta_40: Quantos membros do Comitê de Auditoria são conselheiros independentes?
 * - Resposta_41: O Comitê de Auditoria é coordenado por um conselheiro independente?
 * - Resposta_47: A empresa contrata seguro D&O para seus conselheiros e diretores?
 * - Resposta_63: O emissor identificou casos de desvios, fraudes, irregularidades e atos ilícitos praticados contra a administração pública?
 */
public class CompanyResponse {
    
    private String nomeEmpresa;
    private String resposta02;  // Receita líquida
    private String resposta03;  // Lucro líquido
    private String resposta05;  // Firma de auditoria
    private String resposta06;  // Gastos anuais com auditoria
    private String resposta08;  // Gastos com serviços adicionais
    private String resposta10;  // Política de gerenciamento de riscos
    private String resposta14;  // Existência de auditoria interna
    private String resposta15;  // Sistema de controles internos adequado
    private String resposta16;  // Deficiências/recomendações sobre controles internos
    private String resposta18;  // Divulgação de informações ASG
    private String resposta19;  // Conselho Fiscal instalado
    private String resposta23;  // Quantidade de Comitês do Conselho de Administração
    private String resposta27;  // Regras de conflitos de interesses
    private String resposta30;  // Membros do Conselho de Administração
    private String resposta31;  // Mulheres no Conselho de Administração
    private String resposta32;  // Conselheiros externos
    private String resposta33;  // Conselheiros independentes
    private String resposta34;  // Conselheiros executivos
    private String resposta38;  // Membros do Comitê de Auditoria
    private String resposta39;  // Membros do Comitê de Auditoria que são conselheiros
    private String resposta40;  // Membros do Comitê de Auditoria que são conselheiros independentes
    private String resposta41;  // Comitê de Auditoria coordenado por conselheiro independente
    private String resposta47;  // Seguro D&O
    private String resposta63;  // Casos de desvios/fraudes
    
    public CompanyResponse() {
    }
    
    public CompanyResponse(String nomeEmpresa) {
        this.nomeEmpresa = nomeEmpresa;
    }
    
    // Getters e Setters
    
    public String getNomeEmpresa() {
        return nomeEmpresa;
    }
    
    public void setNomeEmpresa(String nomeEmpresa) {
        this.nomeEmpresa = nomeEmpresa;
    }
    
    public String getResposta02() {
        return resposta02;
    }
    
    public void setResposta02(String resposta02) {
        this.resposta02 = resposta02;
    }
    
    public String getResposta03() {
        return resposta03;
    }
    
    public void setResposta03(String resposta03) {
        this.resposta03 = resposta03;
    }
    
    public String getResposta05() {
        return resposta05;
    }
    
    public void setResposta05(String resposta05) {
        this.resposta05 = resposta05;
    }
    
    public String getResposta06() {
        return resposta06;
    }
    
    public void setResposta06(String resposta06) {
        this.resposta06 = resposta06;
    }
    
    public String getResposta08() {
        return resposta08;
    }
    
    public void setResposta08(String resposta08) {
        this.resposta08 = resposta08;
    }
    
    public String getResposta10() {
        return resposta10;
    }
    
    public void setResposta10(String resposta10) {
        this.resposta10 = resposta10;
    }
    
    public String getResposta14() {
        return resposta14;
    }
    
    public void setResposta14(String resposta14) {
        this.resposta14 = resposta14;
    }
    
    public String getResposta15() {
        return resposta15;
    }
    
    public void setResposta15(String resposta15) {
        this.resposta15 = resposta15;
    }
    
    public String getResposta16() {
        return resposta16;
    }
    
    public void setResposta16(String resposta16) {
        this.resposta16 = resposta16;
    }
    
    public String getResposta18() {
        return resposta18;
    }
    
    public void setResposta18(String resposta18) {
        this.resposta18 = resposta18;
    }
    
    public String getResposta19() {
        return resposta19;
    }
    
    public void setResposta19(String resposta19) {
        this.resposta19 = resposta19;
    }
    
    public String getResposta23() {
        return resposta23;
    }
    
    public void setResposta23(String resposta23) {
        this.resposta23 = resposta23;
    }
    
    public String getResposta27() {
        return resposta27;
    }
    
    public void setResposta27(String resposta27) {
        this.resposta27 = resposta27;
    }
    
    public String getResposta30() {
        return resposta30;
    }
    
    public void setResposta30(String resposta30) {
        this.resposta30 = resposta30;
    }
    
    public String getResposta31() {
        return resposta31;
    }
    
    public void setResposta31(String resposta31) {
        this.resposta31 = resposta31;
    }
    
    public String getResposta32() {
        return resposta32;
    }
    
    public void setResposta32(String resposta32) {
        this.resposta32 = resposta32;
    }
    
    public String getResposta33() {
        return resposta33;
    }
    
    public void setResposta33(String resposta33) {
        this.resposta33 = resposta33;
    }
    
    public String getResposta34() {
        return resposta34;
    }
    
    public void setResposta34(String resposta34) {
        this.resposta34 = resposta34;
    }
    
    public String getResposta38() {
        return resposta38;
    }
    
    public void setResposta38(String resposta38) {
        this.resposta38 = resposta38;
    }
    
    public String getResposta39() {
        return resposta39;
    }
    
    public void setResposta39(String resposta39) {
        this.resposta39 = resposta39;
    }
    
    public String getResposta40() {
        return resposta40;
    }
    
    public void setResposta40(String resposta40) {
        this.resposta40 = resposta40;
    }
    
    public String getResposta41() {
        return resposta41;
    }
    
    public void setResposta41(String resposta41) {
        this.resposta41 = resposta41;
    }
    
    public String getResposta47() {
        return resposta47;
    }
    
    public void setResposta47(String resposta47) {
        this.resposta47 = resposta47;
    }
    
    public String getResposta63() {
        return resposta63;
    }
    
    public void setResposta63(String resposta63) {
        this.resposta63 = resposta63;
    }
    
    /**
     * Define resposta para uma questão específica.
     * 
     * @param numeroQuestao Número da questão (2, 3, 5, 6, 8, 10, 14, 15, 16, 18, 19, 23, 27, 30, 31, 32, 33, 34, 38, 39, 40, 41, 47, 63)
     * @param resposta Resposta da questão
     */
    public void setResposta(int numeroQuestao, String resposta) {
        switch (numeroQuestao) {
            case 2 -> this.resposta02 = resposta;
            case 3 -> this.resposta03 = resposta;
            case 5 -> this.resposta05 = resposta;
            case 6 -> this.resposta06 = resposta;
            case 8 -> this.resposta08 = resposta;
            case 10 -> this.resposta10 = resposta;
            case 14 -> this.resposta14 = resposta;
            case 15 -> this.resposta15 = resposta;
            case 16 -> this.resposta16 = resposta;
            case 18 -> this.resposta18 = resposta;
            case 19 -> this.resposta19 = resposta;
            case 23 -> this.resposta23 = resposta;
            case 27 -> this.resposta27 = resposta;
            case 30 -> this.resposta30 = resposta;
            case 31 -> this.resposta31 = resposta;
            case 32 -> this.resposta32 = resposta;
            case 33 -> this.resposta33 = resposta;
            case 34 -> this.resposta34 = resposta;
            case 38 -> this.resposta38 = resposta;
            case 39 -> this.resposta39 = resposta;
            case 40 -> this.resposta40 = resposta;
            case 41 -> this.resposta41 = resposta;
            case 47 -> this.resposta47 = resposta;
            case 63 -> this.resposta63 = resposta;
            default -> throw new IllegalArgumentException("Questão inválida: " + numeroQuestao);
        }
    }
    
    /**
     * Converte para linha CSV com separador ;
     * 
     * Formato: Nome_Empresa;Resposta_02_RAG;;Resposta_03_RAG;;...
     * Cada questão tem duas colunas: _RAG (preenchida automaticamente) e _Manual (vazia para conferência)
     * 
     * @return String com todas as respostas separadas por ;
     */
    public String toCsvLine() {
        return String.format("%s;%s;;%s;;%s;;%s;;%s;;%s;;%s;;%s;;%s;;%s;;%s;;%s;;%s;;%s;;%s;;%s;;%s;;%s;;%s;;%s;;%s;;%s;;%s;;%s;", 
            nomeEmpresa != null ? nomeEmpresa : "N/A",
            resposta02 != null ? resposta02 : "",
            resposta03 != null ? resposta03 : "",
            resposta05 != null ? resposta05 : "",
            resposta06 != null ? resposta06 : "",
            resposta08 != null ? resposta08 : "",
            resposta10 != null ? resposta10 : "",
            resposta14 != null ? resposta14 : "",
            resposta15 != null ? resposta15 : "",
            resposta16 != null ? resposta16 : "",
            resposta18 != null ? resposta18 : "",
            resposta19 != null ? resposta19 : "",
            resposta23 != null ? resposta23 : "",
            resposta27 != null ? resposta27 : "",
            resposta30 != null ? resposta30 : "",
            resposta31 != null ? resposta31 : "",
            resposta32 != null ? resposta32 : "",
            resposta33 != null ? resposta33 : "",
            resposta34 != null ? resposta34 : "",
            resposta38 != null ? resposta38 : "",
            resposta39 != null ? resposta39 : "",
            resposta40 != null ? resposta40 : "",
            resposta41 != null ? resposta41 : "",
            resposta47 != null ? resposta47 : "",
            resposta63 != null ? resposta63 : ""
        );
    }
    
    /**
     * Cabeçalho do CSV
     * 
     * Formato: Nome_Empresa;Resposta_02_RAG;Resposta_02_Manual;Resposta_03_RAG;Resposta_03_Manual;...
     * 
     * Cada questão tem duas colunas:
     * - _RAG: Resposta gerada automaticamente pelo sistema
     * - _Manual: Coluna vazia para preenchimento manual e conferência
     * 
     * @return String com nomes das colunas
     */
    public static String csvHeader() {
        return "Nome_Empresa;" +
               "Resposta_02_RAG;Resposta_02_Manual;" +
               "Resposta_03_RAG;Resposta_03_Manual;" +
               "Resposta_05_RAG;Resposta_05_Manual;" +
               "Resposta_06_RAG;Resposta_06_Manual;" +
               "Resposta_08_RAG;Resposta_08_Manual;" +
               "Resposta_10_RAG;Resposta_10_Manual;" +
               "Resposta_14_RAG;Resposta_14_Manual;" +
               "Resposta_15_RAG;Resposta_15_Manual;" +
               "Resposta_16_RAG;Resposta_16_Manual;" +
               "Resposta_18_RAG;Resposta_18_Manual;" +
               "Resposta_19_RAG;Resposta_19_Manual;" +
               "Resposta_23_RAG;Resposta_23_Manual;" +
               "Resposta_27_RAG;Resposta_27_Manual;" +
               "Resposta_30_RAG;Resposta_30_Manual;" +
               "Resposta_31_RAG;Resposta_31_Manual;" +
               "Resposta_32_RAG;Resposta_32_Manual;" +
               "Resposta_33_RAG;Resposta_33_Manual;" +
               "Resposta_34_RAG;Resposta_34_Manual;" +
               "Resposta_38_RAG;Resposta_38_Manual;" +
               "Resposta_39_RAG;Resposta_39_Manual;" +
               "Resposta_40_RAG;Resposta_40_Manual;" +
               "Resposta_41_RAG;Resposta_41_Manual;" +
               "Resposta_47_RAG;Resposta_47_Manual;" +
               "Resposta_63_RAG;Resposta_63_Manual";
    }
    
    @Override
    public String toString() {
        return "CompanyResponse{" +
                "nomeEmpresa='" + nomeEmpresa + '\'' +
                ", resposta02='" + resposta02 + '\'' +
                ", resposta03='" + resposta03 + '\'' +
                ", resposta05='" + resposta05 + '\'' +
                ", resposta06='" + resposta06 + '\'' +
                ", resposta08='" + resposta08 + '\'' +
                ", resposta10='" + resposta10 + '\'' +
                ", resposta14='" + resposta14 + '\'' +
                ", resposta15='" + resposta15 + '\'' +
                ", resposta16='" + resposta16 + '\'' +
                ", resposta18='" + resposta18 + '\'' +
                ", resposta19='" + resposta19 + '\'' +
                ", resposta23='" + resposta23 + '\'' +
                ", resposta27='" + resposta27 + '\'' +
                ", resposta30='" + resposta30 + '\'' +
                ", resposta31='" + resposta31 + '\'' +
                ", resposta32='" + resposta32 + '\'' +
                ", resposta33='" + resposta33 + '\'' +
                ", resposta34='" + resposta34 + '\'' +
                ", resposta38='" + resposta38 + '\'' +
                ", resposta39='" + resposta39 + '\'' +
                ", resposta40='" + resposta40 + '\'' +
                ", resposta41='" + resposta41 + '\'' +
                ", resposta47='" + resposta47 + '\'' +
                ", resposta63='" + resposta63 + '\'' +
                '}';
    }
}
