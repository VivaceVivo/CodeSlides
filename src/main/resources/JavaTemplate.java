import java.util.*; 
import java.io.*; 
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.function.*;
import java.util.stream.*;
// IMPORTS_HERE

public class JavaTemplate{
  static void println(Object txt){ 
    if(txt==null){
      System.out.println("null");
    } else {
      System.out.println(txt.toString());
    }  
  }
  
  static <T> List<T> List(T ... elements){
    List<T> result = new ArrayList<>();
    for(T t : elements){
      result.add(t);
    }
    return result;
  }
  
  static{
  	JavaTemplate template = new JavaTemplate();
	template.execute();
  }
  
  // METHODS_HERE
   
  public void execute(){
    try{
      // CODE_HERE
    }catch(Exception x){
      x.printStackTrace();
    }
  }
  
}