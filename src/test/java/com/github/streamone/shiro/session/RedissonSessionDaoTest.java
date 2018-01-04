package com.github.streamone.shiro.session;

import org.apache.shiro.session.mgt.SimpleSession;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import static org.junit.Assert.*;

/**
 * <p>RedissonSessionDao test case.</p>
 *
 * @author streamone
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/sessionContext.xml")
public class RedissonSessionDaoTest {

    @Resource(name = "sessionDao")
    private RedissonSessionDao sessionDao;

    @Test
    public void testDeleteNull() {
        this.sessionDao.delete(null);
        this.sessionDao.delete(new SimpleSession());
    }

    @Test
    public void testGetRedisson() {
        assertNotNull(this.sessionDao.getRedisson());
    }

    @Test
    public void testGetActiveSessions() {
        assertTrue(this.sessionDao.getActiveSessions().isEmpty());
    }
}
