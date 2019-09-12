package br.com.devmedia.webservice.resources.filter;

import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.internal.util.Base64;

import br.com.devmedia.webservice.domain.ErrorMessage;
import br.com.devmedia.webservice.domain.Tipo;
import br.com.devmedia.webservice.domain.Usuario;
import br.com.devmedia.webservice.service.UsuarioService;

// 1. Esta classe 'implements ContainerRequestFilter' permite a implementa��o do m�todo filter
// 2. Recupar os dados de usu�rio e senha enviados no cabe�alho da requisi��o e verifica se s�o v�lidos.
// 3. ResourceInfo � uma interface da API Jax-Rs que nos permite recuperar a classe e o nome do recurso
//    solicitado na requisi��o.
// 4. Ao injetarmos o recurso com o @Context, estamos informando ao Jersey que ele deve injetar a inst�ncia
//    de ResourceInfo nesse atributo. Em outras palavras, com esse c�digo j� temos como saber a classe do
//    recurso e o m�todo que deve ser executado para atender a uma requisi��o.

@Provider
@AcessoRestrito
public class AutenticacaoEAutorizacaoFilter implements ContainerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BASIC_AUTHORIZATION_PREFIX = "Basic ";

    private UsuarioService usuarioService = new UsuarioService();

    @Context
    private ResourceInfo resourceInfo;
    
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        List<String> headersAutorizacao = requestContext.getHeaders().get(AUTHORIZATION_HEADER);
        if((headersAutorizacao != null) && (headersAutorizacao.size() > 0)) {
            String dadosAutorizacao = headersAutorizacao.get(0);
            Usuario usuarioDoHeader = obterUsuarioDoHeader(dadosAutorizacao);

            Usuario usuarioAutenticado = usuarioService.autenticarUsuario(usuarioDoHeader);
            if (usuarioAutenticado != null) {
            	
            	// Aqui significa que o usu�rio est� autenticado. Logo, checar se o usu�rio tem a permiss�o
            	// de acessar o recurso.
            	
            	autorizarUsuario ( requestContext, usuarioAutenticado );
            	
                return;
            }
        }

        Response naoAutorizado = Response.status(Response.Status.UNAUTHORIZED)
                .entity(new ErrorMessage("Usuário não autenticado. Verifique os dados de login e senha.",
                        Response.Status.UNAUTHORIZED.getStatusCode()))
                .build();

        requestContext.abortWith(naoAutorizado);
    }

    private void autorizarUsuario(ContainerRequestContext requestContext, Usuario usuarioAutenticado) {

    	// Exemplo: se uma requisi��o do tipo GET for enviado para /exemplo/webapi/usuarios/1/imoveis
    	// o valor armazenado em classeDoRecurso ser� a classe Imovelresource.
    	
    	Class<?> classeDoRecurso = resourceInfo.getResourceClass();	// recuperar a classe do recurso acessado
    	List<Tipo> permissoesDaClasse = recuperarPermissoes(classeDoRecurso);	// recuperar a lista de permiss�es
    	
    	Method metodoDoRecurso = resourceInfo.getResourceMethod();
    	List<Tipo> permissoesDoMetodo = recuperarPermissoes(metodoDoRecurso);
    	
    	try {
            if (permissoesDoMetodo.isEmpty()) {
                verificarPermissoes(permissoesDaClasse, requestContext, usuarioAutenticado);
            } else {
                verificarPermissoes(permissoesDoMetodo, requestContext, usuarioAutenticado);
            }

        } catch (Exception ex) {
        	// status 403 - FORBIDDEN
            requestContext.abortWith(
                    Response.status(Response.Status.FORBIDDEN)
                            .entity(new ErrorMessage("Usu�rio n�o tem permiss�o para executar essa fun��o.",
                                    Response.Status.FORBIDDEN.getStatusCode()))
                            .build());
        }
		
	}

    // AnnotatedElement � uma interface que pode receber qualquer elemento decorado com uma anota��o (classe ou m�todo).
    
	private void verificarPermissoes(List<Tipo> permissoes, ContainerRequestContext requestContext,
			Usuario usuario) throws Exception {
		
		if (permissoes.contains(usuario.getTipo())) {
			long idUsuarioAcessado = recuperarIdDaURL(requestContext);
			if ((Tipo.CLIENTE.equals(usuario.getTipo())) && (usuario.getId() == idUsuarioAcessado)) {
				// Sevir� para testar que usu�rio do tipo CLIENTE acesse apenas informa��es a ele relacionadas.
                return;
            } else if (!Tipo.CLIENTE.equals(usuario.getTipo())) {
                return;
            }
		}
		throw new Exception();
		
	}

	private long recuperarIdDaURL(ContainerRequestContext requestContext) {
		
		// requisi��o = /exemplo/webapi/usuarios/3/imoveis ser� retornado '3' 
		
		String idObtidoDaURL = requestContext.getUriInfo().getPathParameters().getFirst("usuarioId");
		if (idObtidoDaURL == null) {
			return 0;
		} else {
			return Long.parseLong(idObtidoDaURL);
		}
		
	}

	private List<Tipo> recuperarPermissoes(AnnotatedElement elementoAnotado) {

		AcessoRestrito acessoRestrito = elementoAnotado.getAnnotation(AcessoRestrito.class);
		if (acessoRestrito == null) {
			// o elemento n�o recebeu a anota��o.
			return new ArrayList<Tipo>();	// lista vazia.
		} else {
			Tipo[] permissoes = acessoRestrito.value();		// recebe array de valores;
			return Arrays.asList(permissoes);
		}
		
	}

	private Usuario obterUsuarioDoHeader(String dadosAutorizacao) {
        dadosAutorizacao = dadosAutorizacao.replaceFirst(BASIC_AUTHORIZATION_PREFIX, "");
        String dadosDecodificados = Base64.decodeAsString(dadosAutorizacao);
        StringTokenizer dadosTokenizer = new StringTokenizer(dadosDecodificados,":");
        Usuario usuario = new Usuario();
        usuario.setUsername(dadosTokenizer.nextToken());
        usuario.setPassword(dadosTokenizer.nextToken());
        return usuario;
    }

}
