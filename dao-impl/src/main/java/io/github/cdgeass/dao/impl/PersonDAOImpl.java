package io.github.cdgeass.dao.impl;

import io.github.cdgeass.dao.api.AddressDAO;
import io.github.cdgeass.dao.api.PersonDAO;
import io.github.cdgeass.dao.dto.PersonDTO;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.transaction.control.TransactionControl;
import org.osgi.service.transaction.control.jdbc.JDBCConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static io.github.cdgeass.dao.impl.PersonTable.*;

/**
 * @author cdeass
 * @since  2020-10-28
 */
@Component
public class PersonDAOImpl implements PersonDAO {

    private static final Logger logger = LoggerFactory.getLogger(PersonDAOImpl.class);

    @Reference
    TransactionControl transactionControl;

    @Reference(name = "provider")
    JDBCConnectionProvider jdbcConnectionProvider;

    @Reference
    AddressDAO addressDAO;

    Connection connection;

    @Activate
    void start(Map<String, Object> props) throws SQLException {
        connection = jdbcConnectionProvider.getResource(transactionControl);
        transactionControl.supports(() -> connection.prepareStatement(INIT).execute());
    }

    @Override
    public List<PersonDTO> select() {

        return transactionControl.notSupported(() -> {

            List<PersonDTO> dbResults = new ArrayList<>();

            ResultSet rs = connection.createStatement().executeQuery(SQL_SELECT_ALL_PERSONS);

            while (rs.next()) {
                PersonDTO personDTO = mapRecordToPerson(rs);
                personDTO.addresses = addressDAO.select(personDTO.personId);
                dbResults.add(personDTO);
            }

            return dbResults;
        });
    }

    @Override
    public PersonDTO findByPK(Long pk) {

        return transactionControl.supports(() -> {

            PersonDTO personDTO = null;

            PreparedStatement pst = connection.prepareStatement(SQL_SELECT_PERSON_BY_PK);
            pst.setLong(1, pk);

            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                personDTO = mapRecordToPerson(rs);
                personDTO.addresses = addressDAO.select(pk);
            }

            return personDTO;
        });
    }

    @Override
    public Long save(PersonDTO data) {

        return transactionControl.required(() -> {

            PreparedStatement pst = connection.prepareStatement(SQL_INSERT_PERSON, Statement.RETURN_GENERATED_KEYS);

            pst.setString(1, data.firstName);
            pst.setString(2, data.lastName);

            pst.executeUpdate();

            AtomicLong genPersonId = new AtomicLong(data.personId);

            if (genPersonId.get() <= 0) {
                ResultSet genKeys = pst.getGeneratedKeys();

                if (genKeys.next()) {
                    genPersonId.set(genKeys.getLong(1));
                }
            }

            logger.info("Saved Person With ID : {}", genPersonId.get());

            if (genPersonId.get() > 0) {
                data.addresses.forEach(address -> {
                    address.personId = genPersonId.get();
                    addressDAO.save(genPersonId.get(), address);
                });
            }

            return genPersonId.get();
        });
    }

    @Override
    public void update(PersonDTO data) {

        transactionControl.required(() -> {

            PreparedStatement pst = connection.prepareStatement(SQL_UPDATE_PERSON_BY_PK);
            pst.setString(1, data.firstName);
            pst.setString(2, data.lastName);
            pst.setLong(3, data.personId);
            pst.executeUpdate();

            logger.info("Updated person : {}", data);

            data.addresses.forEach(address -> addressDAO.update(data.personId, address));

            return null;
        });
    }

    @Override
    public void delete(Long pk) {

        transactionControl.required(() -> {
            PreparedStatement pst = connection.prepareStatement(SQL_DELETE_PERSON_BY_PK);
            pst.setLong(1, pk);
            pst.executeUpdate();
            addressDAO.delete(pk);
            logger.info("Deleted Person with ID : {}", pk);
            return null;
        });
    }

    protected PersonDTO mapRecordToPerson(ResultSet rs) throws SQLException {
        PersonDTO personDTO = new PersonDTO();
        personDTO.personId = rs.getLong(PERSON_ID);
        personDTO.firstName = rs.getString(FIRST_NAME);
        personDTO.lastName = rs.getString(LAST_NAME);
        return personDTO;
    }
}
