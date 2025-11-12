package com.example.rag.automation.model;

/**
 * Representa uma questão do Guia de Coleta.
 * 
 * Estrutura do CSV:
 * Nº;Dificuldade;Questão;Onde?;Como Preencher?;OBSERVAÇÕES;Tipo
 */
public class Question {
    
    private int numero;
    private String dificuldade;
    private String questao;
    private String onde;
    private String comoPreencher;
    private String observacoes;
    private TipoQuestao tipo;
    private String palavrasChaveRag;
    
    public Question() {
    }
    
    public Question(int numero, String dificuldade, String questao, 
                    String onde, String comoPreencher, String observacoes, TipoQuestao tipo, String palavrasChaveRag) {
        this.numero = numero;
        this.dificuldade = dificuldade;
        this.questao = questao;
        this.onde = onde;
        this.comoPreencher = comoPreencher;
        this.observacoes = observacoes;
        this.tipo = tipo;
        this.palavrasChaveRag = palavrasChaveRag;
    }

    public String getPalavrasChaveRag() {
        return palavrasChaveRag;
    }

    public void setPalavrasChaveRag(String palavrasChaveRag) {
        this.palavrasChaveRag = palavrasChaveRag;
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
    
    public TipoQuestao getTipo() {
        return tipo;
    }
    
    public void setTipo(TipoQuestao tipo) {
        this.tipo = tipo;
    }
    
    @Override
    public String toString() {
        return "Question{" +
                "numero=" + numero +
                ", dificuldade='" + dificuldade + '\'' +
                ", questao='" + questao + '\'' +
                ", onde='" + onde + '\'' +
                ", tipo=" + tipo +
                '}';
    }
}
