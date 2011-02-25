/**
 * Copyright (C) 2010 Peter Karich <jetwick_@_pannous_._info>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.jetwick.ui;

import de.jetwick.es.TweetQuery;
import java.util.Collection;
import com.google.inject.Guice;
import com.google.inject.Injector;
import de.jetwick.config.Configuration;
import de.jetwick.config.DefaultModule;
import de.jetwick.es.ElasticTweetSearch;
import de.jetwick.es.ElasticUserSearch;
import de.jetwick.rmi.RMIClient;
import de.jetwick.data.JUser;
import de.jetwick.tw.TwitterSearch;
import de.jetwick.tw.queue.TweetPackage;
import java.rmi.RemoteException;
import java.util.ArrayList;
import org.apache.wicket.Application;
import org.apache.wicket.guice.GuiceComponentInjector;
import org.apache.wicket.util.tester.WicketTester;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.ShardSearchFailure;
import org.elasticsearch.search.facet.InternalFacets;
import org.elasticsearch.search.internal.InternalSearchHit;
import org.elasticsearch.search.internal.InternalSearchHits;
import org.elasticsearch.search.internal.InternalSearchResponse;
import org.junit.Before;
import twitter4j.TwitterException;
import static org.mockito.Mockito.*;

public class WicketPagesTestClass {

    protected WicketTester tester;
    protected Injector injector;

    @Before
    public void setUp() throws Exception {
        tester = new WicketTester(createJetwickApp());
    }

    protected <T> T getInstance(Class<T> clazz) {
        return injector.getInstance(clazz);
    }

    protected JetwickApp createJetwickApp() {
        DefaultModule mod = new DefaultModule() {

            @Override
            public void installTwitterModule() {
                bind(TwitterSearch.class).toInstance(createTestTwitterSearch());
            }

//            @Override
//            public void installDbModule() {                
//                WorkManager db = mock(WorkManager.class);
//                bind(WorkManager.class).toInstance(db);
//                TagDao tagDao = mock(TagDao.class);
//                bind(TagDao.class).toInstance(tagDao);
//                UserDao userDao = mock(UserDao.class);
//                bind(UserDao.class).toInstance(userDao);
//            }
            @Override
            public void installSearchModule() {
                ElasticUserSearch userSearch = mock(ElasticUserSearch.class);
                bind(ElasticUserSearch.class).toInstance(userSearch);

                ElasticTweetSearch twSearch = mock(ElasticTweetSearch.class);
                
                // mock this hit/result too!
                //new InternalSearchHit(1, "1", "tweet", source, fields);
                InternalSearchResponse iRsp = new InternalSearchResponse(
                        new InternalSearchHits(new InternalSearchHit[0], 0, 0), new InternalFacets(new ArrayList()), true);
                when(twSearch.search((Collection<JUser>) any(), (TweetQuery) any())).
                        thenReturn(new SearchResponse(iRsp, "", 4, 4, 1L, new ShardSearchFailure[0]));

                bind(ElasticTweetSearch.class).toInstance(twSearch);
            }

            @Override
            public void installRMIModule() {
                bind(RMIClient.class).toInstance(createRMIClient());
            }
        };
        injector = Guice.createInjector(mod);
        return new JetwickApp(injector) {

            @Override
            public String getConfigurationType() {
                return Application.DEVELOPMENT;
            }

            @Override
            protected GuiceComponentInjector getGuiceInjector() {
                return new GuiceComponentInjector(this, injector);
            }
        };
    }

    protected TwitterSearch createTestTwitterSearch() {
        return new TwitterSearch() {
            
            @Override
            public int getRateLimit() {
                return 100;
            }

            @Override
            public TwitterSearch initTwitter4JInstance(String t, String ts) {
                return this;
            }

            @Override
            public JUser getUser() throws TwitterException {
                return new JUser("testUser");
            }
        }.setConsumer("", "");
    }

    protected RMIClient createRMIClient() {
        return new RMIClient(new Configuration()) {

            @Override
            public RMIClient init() {
                return this;
            }

            @Override
            public void send(TweetPackage tweets) throws RemoteException {
                // disable rmi stuff
            }
        };
    }
}
