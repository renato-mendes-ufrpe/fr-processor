package com.example.rag.automation.model;

/**
 * Representa a resposta de uma empresa para as questões do guia.
 * 
 * Formato do CSV de saída:
 * Nome_Empresa;Resposta_02;Resposta_03;Resposta_05;Resposta_06;Resposta_08
 * 
 * Questões correspondentes:
 * - Resposta_02: Qual é a receita líquida da empresa?
 * - Resposta_03: Qual é o lucro líquido da empresa?
 * - Resposta_05: Qual é a firma de auditoria independente?
 * - Resposta_06: Qual o valor dos gastos anuais com a empresa de auditoria independente?
 * - Resposta_08: Qual o valor dos eventuais gastos com serviços adicionais prestados pela firma de auditoria independente?
 */
public class CompanyResponse {
    
    private String nomeEmpresa;
    private String resposta02;  // Receita líquida
    private String resposta03;  // Lucro líquido
    private String resposta05;  // Firma de auditoria
    private String resposta06;  // Gastos anuais com auditoria
    private String resposta08;  // Gastos com serviços adicionais
    
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
    
    /**
     * Define resposta para uma questão específica.
     * 
     * @param numeroQuestao Número da questão (2, 3, 5, 6, 8)
     * @param resposta Resposta da questão
     */
    public void setResposta(int numeroQuestao, String resposta) {
        switch (numeroQuestao) {
            case 2 -> this.resposta02 = resposta;
            case 3 -> this.resposta03 = resposta;
            case 5 -> this.resposta05 = resposta;
            case 6 -> this.resposta06 = resposta;
            case 8 -> this.resposta08 = resposta;
            default -> throw new IllegalArgumentException("Questão inválida: " + numeroQuestao);
        }
    }
    
    /**
     * Converte para linha CSV com separador ;
     * 
     * @return String no formato: Nome_Empresa;Resposta_02;Resposta_03;Resposta_05;Resposta_06;Resposta_08
     */
    public String toCsvLine() {
        return String.format("%s;%s;%s;%s;%s;%s", 
            nomeEmpresa != null ? nomeEmpresa : "N/A",
            resposta02 != null ? resposta02 : "N/A",
            resposta03 != null ? resposta03 : "N/A",
            resposta05 != null ? resposta05 : "N/A",
            resposta06 != null ? resposta06 : "N/A",
            resposta08 != null ? resposta08 : "N/A"
        );
    }
    
    /**
     * Cabeçalho do CSV
     * 
     * @return String com nomes das colunas
     */
    public static String csvHeader() {
        return "Nome_Empresa;Resposta_02;Resposta_03;Resposta_05;Resposta_06;Resposta_08";
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
                '}';
    }
}
