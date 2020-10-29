package io.github.cdgeass.dao.api;

import io.github.cdgeass.dao.dto.PersonDTO;
import org.osgi.annotation.versioning.ProviderType;

import java.util.List;

/**
 * @author cdgeass
 * @since  2020-10-28
 */
@ProviderType
public interface PersonDAO {

    List<PersonDTO> select();

    PersonDTO findByPK(Long pk);

    Long save(PersonDTO data);

    void update(PersonDTO data);

    void delete(Long pk);

}
