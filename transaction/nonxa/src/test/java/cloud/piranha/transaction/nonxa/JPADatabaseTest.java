/*
 * Copyright (c) 2002-2020 Manorrock.com. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 *
 *   1. Redistributions of source code must retain the above copyright notice, 
 *      this list of conditions and the following disclaimer.
 *   2. Redistributions in binary form must reproduce the above copyright notice,
 *      this list of conditions and the following disclaimer in the documentation
 *      and/or other materials provided with the distribution.
 *   3. Neither the name of the copyright holder nor the names of its 
 *      contributors may be used to endorse or promote products derived from this
 *      software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package cloud.piranha.transaction.nonxa;

import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.transaction.Transaction;
import org.h2.jdbcx.JdbcDataSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

/**
 * s
 * A JPA database test.
 *
 * @author Manfred Riem (mriem@manorrock.com)
 */
public class JPADatabaseTest {

    /**
     * Test database.
     *
     * @throws Exception when a serious error occurs.
     */
    @Test
    public void testJpaDatabase() throws Exception {
        System.getProperties().put("java.naming.factory.initial", "cloud.piranha.jndi.memory.DefaultInitialContextFactory");
        InitialContext initialContext = new InitialContext();
        JdbcDataSource dataSource;
        try {
            initialContext.lookup("java:/JPADatabaseTest");
        } catch(NameNotFoundException nnfe) {
            dataSource = new JdbcDataSource();
            dataSource.setURL("jdbc:h2:./target/JPADatabaseTest");initialContext.bind("java:/JPADatabaseTest", dataSource);
        }
        DefaultTransactionManager transactionManager;
        try {
            transactionManager = (DefaultTransactionManager) initialContext.lookup("java:/TransactionManager");
        } catch (NameNotFoundException nnfe) {
            transactionManager = new DefaultTransactionManager();
            initialContext.rebind("java:/TransactionManager", transactionManager);
        }
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("JPADatabaseTest");
        transactionManager.begin();
        EntityManager em = emf.createEntityManager();
        JPATest jpaTest = new JPATest();
        jpaTest.setId(3);
        em.persist(jpaTest);
        em.flush();
        Transaction transaction = transactionManager.getTransaction();
        assertEquals(transaction.getStatus(), 0);
        transactionManager.commit();
        assertEquals(transaction.getStatus(), 3);
        em = emf.createEntityManager();
        JPATest jpaTest2 = em.find(JPATest.class, 3);
        assertNotNull(jpaTest2);
        assertEquals(jpaTest.getId(), jpaTest2.getId());
    }
}
