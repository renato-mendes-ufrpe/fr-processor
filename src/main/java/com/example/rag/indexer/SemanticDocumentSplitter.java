package com.example.rag.indexer;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.segment.TextSegment;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Divisor de documentos customizado que respeita delimitadores semânticos
 * específicos do Formulário de Referência da Ambipar.
 * 
 * Estratégia:
 * 1. Identifica blocos semânticos (registros de pessoas, seções)
 * 2. Agrupa blocos em chunks respeitando o tamanho máximo
 * 3. Garante que cada chunk contenha informação completa (nome + cargo/classificação)
 * 4. Aplica overlap inteligente para não perder contexto entre chunks
 */
public class SemanticDocumentSplitter implements DocumentSplitter {
    
    // Padrões para identificar delimitadores semânticos
    private static final Pattern PERSON_RECORD = Pattern.compile(
        "(?:^|\\n)Nome:?\\s+([\\w\\s]+?)(?:\\s+CPF:|\\n)",
        Pattern.MULTILINE
    );
    
    private static final Pattern SECTION_HEADER = Pattern.compile(
        "(?:^|\\n)\\d+\\.\\d+\\s+[^\\n]{10,80}\\n",
        Pattern.MULTILINE
    );
    
    private static final Pattern PAGE_MARKER = Pattern.compile(
        "PÁGINA:\\s*\\d+\\s*de\\s*\\d+"
    );
    
    private final int maxSegmentSizeInTokens;
    private final int overlapInTokens;
    
    /**
     * Construtor
     * 
     * @param maxSegmentSizeInTokens Tamanho máximo do chunk em tokens
     * @param overlapInTokens Tokens de overlap entre chunks
     */
    public SemanticDocumentSplitter(
            int maxSegmentSizeInTokens,
            int overlapInTokens) {
        this.maxSegmentSizeInTokens = maxSegmentSizeInTokens;
        this.overlapInTokens = overlapInTokens;
    }
    
    @Override
    public List<TextSegment> split(Document document) {
        String text = document.text();
        List<TextSegment> segments = new ArrayList<>();
        
        // Fase 1: Identificar todos os blocos semânticos
        List<SemanticBlock> blocks = identifySemanticBlocks(text);
        
        // Fase 2: Agrupar blocos em chunks respeitando o tamanho máximo
        List<String> chunks = groupBlocksIntoChunks(blocks, text);
        
        // Fase 3: Converter chunks em TextSegments
        for (String chunk : chunks) {
            segments.add(TextSegment.from(chunk.trim()));
        }
        
        return segments;
    }
    
    /**
     * Identifica blocos semânticos no texto (pessoas, seções, etc)
     */
    private List<SemanticBlock> identifySemanticBlocks(String text) {
        List<SemanticBlock> blocks = new ArrayList<>();
        
        // Identifica registros de pessoas (Nome: ... até próximo Nome: ou marcador de página)
        Matcher personMatcher = PERSON_RECORD.matcher(text);
        int lastEnd = 0;
        
        while (personMatcher.find()) {
            int start = personMatcher.start();
            
            // Adiciona bloco anterior (se existir)
            if (start > lastEnd) {
                blocks.add(new SemanticBlock(lastEnd, start, BlockType.OTHER));
            }
            
            // Encontra o fim deste registro (próximo Nome: ou marcador de página)
            int end = findPersonRecordEnd(text, personMatcher.end());
            blocks.add(new SemanticBlock(start, end, BlockType.PERSON_RECORD));
            lastEnd = end;
        }
        
        // Adiciona último bloco
        if (lastEnd < text.length()) {
            blocks.add(new SemanticBlock(lastEnd, text.length(), BlockType.OTHER));
        }
        
        return blocks;
    }
    
    /**
     * Encontra o fim de um registro de pessoa
     */
    private int findPersonRecordEnd(String text, int searchStart) {
        // Procura próximo "Nome:" ou marcador de página
        int nextPerson = text.indexOf("Nome:", searchStart);
        int nextPage = text.indexOf("PÁGINA:", searchStart);
        
        int candidates = Math.min(
            nextPerson != -1 ? nextPerson : Integer.MAX_VALUE,
            nextPage != -1 ? nextPage : Integer.MAX_VALUE
        );
        
        return candidates != Integer.MAX_VALUE ? candidates : text.length();
    }
    
    /**
     * Agrupa blocos semânticos em chunks respeitando o tamanho máximo
     */
    private List<String> groupBlocksIntoChunks(List<SemanticBlock> blocks, String text) {
        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        int currentTokens = 0;
        
        for (SemanticBlock block : blocks) {
            String blockText = text.substring(block.start, block.end);
            int blockTokens = countTokens(blockText);
            
            // Se o bloco sozinho é maior que o máximo, divide ele
            if (blockTokens > maxSegmentSizeInTokens) {
                // Salva chunk atual (se houver)
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString());
                    currentChunk = new StringBuilder();
                    currentTokens = 0;
                }
                
                // Divide o bloco grande usando estratégia recursiva padrão
                chunks.addAll(splitLargeBlock(blockText));
                continue;
            }
            
            // Se adicionar este bloco ultrapassar o limite, cria novo chunk
            if (currentTokens + blockTokens > maxSegmentSizeInTokens && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString());
                
                // Aplica overlap: mantém última parte do chunk anterior
                String overlap = getOverlapText(currentChunk.toString());
                currentChunk = new StringBuilder(overlap);
                currentTokens = countTokens(overlap);
            }
            
            // Adiciona bloco ao chunk atual
            currentChunk.append(blockText);
            currentTokens += blockTokens;
        }
        
        // Adiciona último chunk
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }
        
        return chunks;
    }
    
    /**
     * Divide um bloco grande usando estratégia recursiva padrão
     */
    private List<String> splitLargeBlock(String text) {
        List<String> result = new ArrayList<>();
        
        // Divide por dupla quebra de linha
        String[] paragraphs = text.split("\\n\\n+");
        StringBuilder current = new StringBuilder();
        int currentTokens = 0;
        
        for (String para : paragraphs) {
            int paraTokens = countTokens(para);
            
            if (currentTokens + paraTokens > maxSegmentSizeInTokens && current.length() > 0) {
                result.add(current.toString());
                current = new StringBuilder();
                currentTokens = 0;
            }
            
            current.append(para).append("\n\n");
            currentTokens += paraTokens;
        }
        
        if (current.length() > 0) {
            result.add(current.toString());
        }
        
        return result;
    }
    
    /**
     * Extrai texto de overlap do final de um chunk
     */
    private String getOverlapText(String text) {
        if (text.isEmpty()) {
            return "";
        }
        
        int totalTokens = countTokens(text);
        if (totalTokens <= overlapInTokens) {
            return text;
        }
        
        // Pega aproximadamente overlapInTokens do final
        // Heurística: 1 token ≈ 4 caracteres
        int approxChars = overlapInTokens * 4;
        int startPos = Math.max(0, text.length() - approxChars);
        
        // Ajusta para não cortar no meio de uma palavra
        while (startPos > 0 && !Character.isWhitespace(text.charAt(startPos))) {
            startPos--;
        }
        
        return text.substring(startPos);
    }
    
    /**
     * Conta tokens em um texto usando heurística
     * Aproximação: 1 token ≈ 4 caracteres em português
     */
    private int countTokens(String text) {
        return text.length() / 4;
    }
    
    /**
     * Representa um bloco semântico no texto
     */
    private static class SemanticBlock {
        final int start;
        final int end;
        final BlockType type;
        
        SemanticBlock(int start, int end, BlockType type) {
            this.start = start;
            this.end = end;
            this.type = type;
        }
    }
    
    /**
     * Tipos de blocos semânticos
     */
    private enum BlockType {
        PERSON_RECORD,  // Registro de pessoa (Nome: ... até próximo registro)
        SECTION_HEADER, // Cabeçalho de seção (7.3, 7.4, etc)
        OTHER           // Outros tipos de conteúdo
    }
}
