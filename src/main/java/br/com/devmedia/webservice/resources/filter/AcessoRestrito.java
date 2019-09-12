package br.com.devmedia.webservice.resources.filter;

import javax.ws.rs.NameBinding;

import br.com.devmedia.webservice.domain.Tipo;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

// 1. apenas os usu�rios autenticados ter�o acecsso aos m�todos marcados com esse atributo.
// 2. Esta interface est� marcada na classe AuthenticationFilter.
// 3. Informa que o valor default � um array com os tr�s tipos de usu�rios. Os par�metros devem ser
//    passados quando da anota��o nas classes e m�todos. Exemplo: @AcessoRestrito({Tipo.FUNCIONARIO, Tipo.ADMINISTRADOR})
//    inidicando que apenas funcion�rios e administrador t�m acesso � funcionalidade. Se n�o for informado, 
//    por default, os tr�s tipos de usu�rios poder�o acessar a funcionalidade.

@NameBinding
@Retention(RUNTIME)
@Target({TYPE, METHOD})			// permite decorar classes e m�todos
public @interface AcessoRestrito { 
	
	Tipo[] value() default { Tipo.CLIENTE, Tipo.FUNCIONARIO, Tipo.ADMINISTRADOR };
}
