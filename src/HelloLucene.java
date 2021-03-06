
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.es.SpanishLightStemFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;

public class HelloLucene {
    private static CharArraySet stopSet;
	private static String documentPath = "C:\\Users\\raul.barth\\Downloads\\efe94\\efe2\\";
	private static String queryPath = "C:\\Users\\raul.barth\\Downloads\\Consultas\\Consultas.txt";
	private static String outputPath = "C:\\Users\\raul.barth\\Downloads\\LuceneResult.txt";
	
	public static void main(String[] args) throws IOException, ParseException {
		
		if (args.length < 3) {
			System.out.println("Para executar a aplicaçao," +
					" é necessario passar tres argumentos: documentPath, queryPath e outputPath.");
			System.exit(1);
		}
		
		documentPath = args[0];
		queryPath = args[1];
		outputPath = args[2];
            
		createStopWordsList();
                                       
	    SpanishAnalyzer analyzer = new SpanishAnalyzer();
	
	    // 1. create the index
	    Directory index = new RAMDirectory();	
	    IndexWriterConfig config = new IndexWriterConfig( analyzer);
	
	    IndexWriter w = new IndexWriter(index, config);
	    DocumentParser docParser = new DocumentParser();
	    addDocCollection(w, docParser);    
	    w.close();
	    
	    File queryFile = new File(queryPath); 
	    List<TextQuery> queries = docParser.parseQueries(queryFile);
	    
	    IndexReader reader = DirectoryReader.open(index);
	    IndexSearcher searcher = new IndexSearcher(reader);
	    
	    int queryCount = 1;
	    
	    Writer fileWriter = null;
	    
	    try 
		{
			fileWriter = new BufferedWriter(new OutputStreamWriter(
		            new FileOutputStream(outputPath), "utf-8"));
		} 
		catch (IOException ex) {
		    System.out.println("Arquivo de saida nao pode ser criado! " +
		    		"Sistema ira somente printar os resultados no console.");
		}
	    
	    for (TextQuery query: queries) {
		    int hitsPerPage = 100;
		    TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage);
		    
		    String queryTitle = tokenizeText("title", query.getESTitle());
		    String queryContent = tokenizeText("content", query.getESDesc() + " " + query.getESNarr());
		    Query q = buildQuery(queryTitle, queryContent);
		
		    // search
		    searcher.search(q, collector);
		    ScoreDoc[] hits = collector.topDocs().scoreDocs;
		   
		    // display results
			String queryNum = query.getNum().substring(1);
			
		    for(int i=0;i<hits.length;++i) {
		      int docId = hits[i].doc;
		      float docScore = hits[i].score;
		      
		      Document d = searcher.doc(docId);
		      System.out.println(queryNum +
		    		  "	Q0	" +
		    		  d.get("docno") +
		    		  "	" +
		    		  i + 
		    		  "	" +
		    		  docScore +
		    		  "	Ricardo e Raul");
		      
		      if (fileWriter != null) {
		    	  // if it isn't the first line in the file, print new line
		    	  if(queryCount != 1 || i != 0) {
		    		  fileWriter.append("\n");
		    	  }
		    	  
		    	  fileWriter.append(queryNum +
			    		  "	Q0	" +
			    		  d.get("docno") +
			    		  "	" +
			    		  i + 
			    		  "	" +
			    		  Float.toString(docScore) +
			    		  "	" +
			    		  "Ricardo e Raul");
		      }
		      
		    }
		    queryCount++;
	    }
	    reader.close();
	    
	    if (fileWriter != null) {
		    fileWriter.close();
	    }
  }

  private static void addDocCollection(IndexWriter w, DocumentParser docParser) throws IOException {    
    File dir = new File(documentPath); 
    File[] files = dir.listFiles();
	for (File file : files) {		
		for (SaxDocument saxDocument : docParser.parseDocument(file)) {
			Document document = new Document();

			String path = file.getCanonicalPath();
		    // use a string field for path because we don't want it tokenized
			document.add(new StringField("path", path, Field.Store.NO));
			
			String tokenizedText = tokenizeText("text", saxDocument.getText());
			String tokenizedTitle = tokenizeText("title", saxDocument.getTitle());
			String searchableContent = tokenizedText + " " + tokenizedTitle;
			
			
			document.add(new TextField("text", tokenizedText, Field.Store.YES));                      
			document.add(new TextField("title", tokenizedTitle, Field.Store.YES));
			document.add(new TextField("content", searchableContent, Field.Store.YES));
			document.add(new StringField("docno", saxDocument.getDocNo(), Field.Store.YES));

			w.addDocument(document);
		}
	}
  }
  
  //Tokenizing and filtering stopwords
  private static String tokenizeText(String field, String text) throws IOException{
	  String tokenizedText = "";
	  Analyzer analyzer = new SpanishAnalyzer();
	  
	  //System.out.println(text); //testar a saída da string
	  
	  TokenStream tokenStream   = analyzer.tokenStream(field, text);
	  tokenStream.reset();
	  
	  tokenStream = new LowerCaseFilter(tokenStream);
	  tokenStream = new ASCIIFoldingFilter(tokenStream);
	  tokenStream = new StopFilter(tokenStream, stopSet); // Stopwords
	  tokenStream = new SpanishLightStemFilter(tokenStream); // Steaming
	  
	  CharTermAttribute cattr = tokenStream.addAttribute(CharTermAttribute.class);
	  while (tokenStream.incrementToken()) {
		  tokenizedText += cattr.toString() + " ";
	      //System.out.println(cattr.toString());
	  }
	  
	  tokenStream.end();
	  tokenStream.close();
	  analyzer.close();
	  return tokenizedText;
  }
  
  private static BooleanQuery buildQuery(String title, String content) throws IOException {
	  BooleanQuery titleQuery = new BooleanQuery();
	  titleQuery.setBoost(2);
	  BooleanQuery contentQuery = new BooleanQuery();
	  Analyzer analyzer = new SpanishAnalyzer();
	  
	  TokenStream tokenStream = analyzer.tokenStream("content", title);
	  tokenStream.reset();
	  CharTermAttribute cattr = tokenStream.addAttribute(CharTermAttribute.class);
	  
	  while(tokenStream.incrementToken()) {
		  titleQuery.add(new TermQuery(new Term("content", cattr.toString())), Occur.SHOULD);
	  }
	  
	  tokenStream.end();
	  tokenStream.close();
	  
	  tokenStream = analyzer.tokenStream("content", content);
	  tokenStream.reset();
	  cattr = tokenStream.addAttribute(CharTermAttribute.class);
	  
	  while(tokenStream.incrementToken()) {
		  contentQuery.add(new TermQuery(new Term("content", cattr.toString())), Occur.SHOULD);
	  }
	  
	  tokenStream.end();
	  tokenStream.close();
	  analyzer.close();
	  
	  BooleanQuery query = new BooleanQuery();
	  query.add(titleQuery, Occur.SHOULD);
	  query.add(contentQuery, Occur.SHOULD);
	  
	  return query;
  }
  
  private static void createStopWordsList(){
      stopSet = CharArraySet.copy(SpanishAnalyzer.getDefaultStopSet());
  }
}