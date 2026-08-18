package poly.edu.quanlynhahang.entity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.*;
@Converter
public class StringListJsonConverter implements AttributeConverter<List<String>,String>{
 private static final ObjectMapper JSON=new ObjectMapper();
 public String convertToDatabaseColumn(List<String> value){try{return JSON.writeValueAsString(value==null?List.of():value);}catch(Exception e){throw new IllegalArgumentException("Danh sách JSON không hợp lệ",e);}}
 public List<String> convertToEntityAttribute(String value){if(value==null||value.isBlank())return new ArrayList<>();try{return new ArrayList<>(JSON.readValue(value,new TypeReference<List<String>>(){}));}catch(Exception e){return new ArrayList<>();}}
}
