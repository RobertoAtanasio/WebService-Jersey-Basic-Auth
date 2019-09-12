package br.com.devmedia.webservice.resources;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import br.com.devmedia.webservice.domain.Imovel;
import br.com.devmedia.webservice.domain.Tipo;
import br.com.devmedia.webservice.resources.filter.AcessoRestrito;
import br.com.devmedia.webservice.service.ImovelService;

// OBS.: Apesar do "erro" apresentado em cadastrarImovel, em tempo de execução o sistema reconhece que o 
//       parâmetro "usuarioId" vem do @Path("{usuarioId}/imoveis") de UsuarioResource

// 1. Aqui em @AcessoRestrito qualquer usuário poderá acessar os métodos desta classe, apenas devem está autenticados.
// 2. Observe que não foi informado o array de permissões.
// 3. Observe que apenas o administrador poderá excluir imóvel.

@AcessoRestrito
@Consumes(MediaType.APPLICATION_JSON + ";charset=utf-8")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ImovelResource {

    private final ImovelService imovelService = new ImovelService();

    @POST
    public Response cadastrarImovel(@PathParam("usuarioId") long donoImovelId, Imovel imovel) {
        imovelService.cadastrarImovel(imovel, donoImovelId);
        return Response.status(Status.CREATED)
                .entity(imovel)
                .build();
    }

    @GET
    public List<Imovel> recuperarImoveis() {
        return imovelService.listarImoveis();
    }

    @GET
    @Path("{imovelId}")
    public Imovel recuperarImovel(@PathParam("imovelId") long id) {
        return imovelService.obterImovel(id);
    }

    @PUT
    @Path("{imovelId}")
    public void atualizarImovel(@PathParam("imovelId") long imovelId, Imovel imovel) {
        imovelService.atualizarImovel(imovelId, imovel);
    }

    @DELETE
    @Path("{imovelId}")
    @AcessoRestrito({Tipo.ADMINISTRADOR})
    public void excluirImovel(@PathParam("imovelId") long id) {
        imovelService.descadastrarImovel(id);
    }

}
