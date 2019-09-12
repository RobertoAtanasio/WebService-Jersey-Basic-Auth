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

// 1. Esta classe 'implements ContainerRequestFilter' permite a implementação do método filter
// 2. Recupar os dados de usuário e senha enviados no cabeçalho da requisição e verifica se são válidos.
// 3. ResourceInfo é uma interface da API Jax-Rs que nos permite recuperar a classe e o nome do recurso
//    solicitado na requisição.
// 4. Ao injetarmos o recurso com o @Context, estamos informando ao Jersey que ele deve injetar a instância
//    de ResourceInfo nesse atributo. Em outras palavras, com esse código já temos como saber a classe do
//    recurso e o método que deve ser executado para atender a uma requisição.

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
            	
            	// Aqui significa que o usuário está autenticado. Logo, checar se o usuário tem a permissão
            	// de acessar o recurso.
            	
            	autorizarUsuario ( requestContext, usuarioAutenticado );
            	
                return;
            }
        }

        Response naoAutorizado = Response.status(Response.Status.UNAUTHORIZED)
                .entity(new ErrorMessage("UsuÃ¡rio nÃ£o autenticado. Verifique os dados de login e senha.",
                        Response.Status.UNAUTHORIZED.getStatusCode()))
                .build();

        requestContext.abortWith(naoAutorizado);
    }

    private void autorizarUsuario(ContainerRequestContext requestContext, Usuario usuarioAutenticado) {

    	// Exemplo: se uma requisição do tipo GET for enviado para /exemplo/webapi/usuarios/1/imoveis
    	// o valor armazenado em classeDoRecurso será a classe Imovelresource.
    	
    	Class<?> classeDoRecurso = resourceInfo.getResourceClass();	// recuperar a classe do recurso acessado
    	List<Tipo> permissoesDaClasse = recuperarPermissoes(classeDoRecurso);	// recuperar a lista de permissões
    	
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
                            .entity(new ErrorMessage("Usuário não tem permissão para executar essa função.",
                                    Response.Status.FORBIDDEN.getStatusCode()))
                            .build());
        }
		
	}

    // AnnotatedElement é uma interface que pode receber qualquer elemento decorado com uma anotação (classe ou método).
    
	private void verificarPermissoes(List<Tipo> permissoes, ContainerRequestContext requestContext,
			Usuario usuario) throws Exception {
		
		if (permissoes.contains(usuario.getTipo())) {
			long idUsuarioAcessado = recuperarIdDaURL(requestContext);
			if ((Tipo.CLIENTE.equals(usuario.getTipo())) && (usuario.getId() == idUsuarioAcessado)) {
				// Sevirá para testar que usuário do tipo CLIENTE acesse apenas informações a ele relacionadas.
                return;
            } else if (!Tipo.CLIENTE.equals(usuario.getTipo())) {
                return;
            }
		}
		throw new Exception();
		
	}

	private long recuperarIdDaURL(ContainerRequestContext requestContext) {
		
		// requisição = /exemplo/webapi/usuarios/3/imoveis será retornado '3' 
		
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
			// o elemento não recebeu a anotação.
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
