package pl.com.app.repository.impl;


import org.springframework.stereotype.Repository;
import pl.com.app.exceptions.ExceptionCode;
import pl.com.app.exceptions.MyException;
import pl.com.app.model.Voter;
import pl.com.app.repository.VoterRepository;
import pl.com.app.repository.generic.AbstractGenericRepository;

import java.util.Optional;

@Repository
public class VoterRepositoryImpl extends AbstractGenericRepository<Voter> implements VoterRepository {

    @Override
    public Optional<Voter> findVoterByPesel(String pesel) {
        try {
            if (pesel == null) {
                throw new NullPointerException("PESEL IS NULL");
            }
            return getEntityManager().createQuery("select v from " + getEntityClass().getCanonicalName()
                    + " v where v.pesel = :pesel", getEntityClass())
                    .setParameter("pesel", pesel)
                    .getResultList()
                    .stream()
                    .findFirst();
        } catch (Exception e) {
            throw new MyException(ExceptionCode.REPOSITORY, e.getMessage());
        }
    }
}
