package io.github.cdgeass.dao.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * @author cdgeass
 * @since  2020-10-28
 */
public class PersonDTO {

    public long personId;

    public String firstName;

    public String lastName;

    public List<AddressDTO> addresses = new ArrayList<>();
}
