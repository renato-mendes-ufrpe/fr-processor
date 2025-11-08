package com.example.rag.automation.model;

/**
 * Representa uma questão do Guia de Coleta.
 * 
 * Estrutura do CSV:
 * Nº;Dificuldade;Questão;Onde?;Como Preencher?;OBSERVAÇÕES
 */
public class Question {
    
    private int numero;
    private String dificuldade;
    private String questao;
    private String onde;
    private String comoPreencher;
    private String observacoes;
    
    public Question() {
    }
    
    public Question(int numero, String dificuldade, String questao, 
                    String onde, String comoPreencher, String observacoes) {
        this.numero = numero;
        this.dificuldade = dificuldade;
        this.questao = questao;
        this.onde = onde;
        this.comoPreencher = comoPreencher;
        this.observacoes = observacoes;
    }
    
    // Getters e Setters
    
    public int getNumero() {
        return numero;
    }
    
    public void setNumero(int numero) {
        this.numero = numero;
    }
    
    public String getDificuldade() {
        return dificuldade;
    }
    
    public void setDificuldade(String dificuldade) {
        this.dificuldade = dificuldade;
    }
    
    public String getQuestao() {
        return questao;
    }
    
    public void setQuestao(String questao) {
        this.questao = questao;
    }
    
    public String getOnde() {
        return onde;
    }
    
    public void setOnde(String onde) {
        this.onde = onde;
    }
    
    public String getComoPreencher() {
        return comoPreencher;
    }
    
    public void setComoPreencher(String comoPreencher) {
        this.comoPreencher = comoPreencher;
    }
    
    public String getObservacoes() {
        return observacoes;
    }
    
    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
    
    @Override
    public String toString() {
        return "Question{" +
                "numero=" + numero +
                ", dificuldade='" + dificuldade + '\'' +
                ", questao='" + questao + '\'' +
                ", onde='" + onde + '\'' +
                '}';
    }
}
