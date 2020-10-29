package io.github.cdgeass.dao.api;

import io.github.cdgeass.dao.dto.AddressDTO;
import org.osgi.annotation.versioning.ProviderType;

import java.util.List;

/**
 * @author cdgeass
 * @since  2020-10-28
 */
@ProviderType
public interface AddressDAO {

    List<AddressDTO> select(Long personId);

    AddressDTO findByPK(String emailAddress);

    void save(Long personId, AddressDTO data);

    void update(Long personId, AddressDTO data);

    void delete(Long personId);
}
