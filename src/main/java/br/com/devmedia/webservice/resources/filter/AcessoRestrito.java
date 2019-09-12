package br.com.devmedia.webservice.resources.filter;

import javax.ws.rs.NameBinding;

import br.com.devmedia.webservice.domain.Tipo;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

// 1. apenas os usuários autenticados terão acecsso aos métodos marcados com esse atributo.
// 2. Esta interface está marcada na classe AuthenticationFilter.
// 3. Informa que o valor default é um array com os três tipos de usuários. Os parâmetros devem ser
//    passados quando da anotação nas classes e métodos. Exemplo: @AcessoRestrito({Tipo.FUNCIONARIO, Tipo.ADMINISTRADOR})
//    inidicando que apenas funcionários e administrador têm acesso à funcionalidade. Se não for informado, 
//    por default, os três tipos de usuários poderão acessar a funcionalidade.

@NameBinding
@Retention(RUNTIME)
@Target({TYPE, METHOD})			// permite decorar classes e métodos
public @interface AcessoRestrito { 
	
	Tipo[] value() default { Tipo.CLIENTE, Tipo.FUNCIONARIO, Tipo.ADMINISTRADOR };
}
