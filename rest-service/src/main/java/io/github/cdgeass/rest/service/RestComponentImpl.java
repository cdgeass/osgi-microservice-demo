package io.github.cdgeass.rest.service;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import io.github.cdgeass.dao.api.PersonDAO;
import io.github.cdgeass.dao.dto.PersonDTO;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardResource;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JSONRequired;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

import java.util.List;

/**
 * @author cdgeass
 * @since  2020-10-29
 */
@Component(service=RestComponentImpl.class)
@JaxrsResource
@Path("person")
@Produces(MediaType.APPLICATION_JSON)
@JSONRequired
@HttpWhiteboardResource(pattern = "/microservice/*", prefix = "static")
public class RestComponentImpl {

    @Reference
    private PersonDAO personDAO;

    @GET
    @Path("{person}")
    public PersonDTO getPerson(@PathParam("person") Long personId) {
        return personDAO.findByPK(personId);
    }

    @GET
    public List<PersonDTO> getPerson() {
        return personDAO.select();
    }

    @DELETE
    @Path("{person}")
    public boolean deletePerson(@PathParam("person") long personId) {
        personDAO.delete(personId);
        return true;
    }

    @POST
    public PersonDTO postPerson(PersonDTO person) {
        if (person.personId > 0) {
            personDAO.update(person);
        } else {
            person.personId = personDAO.save(person);
        }
        return person;
    }
}
