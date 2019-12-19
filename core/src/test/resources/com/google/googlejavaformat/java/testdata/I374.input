import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface MyTypeAnno {}

public class GjfFailure {
  void m(int a @MyTypeAnno []) {}
}