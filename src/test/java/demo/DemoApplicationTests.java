package demo;

import demo.model.Related;
import demo.model.Root;
import demo.repository.RelatedRepository;
import demo.repository.RootRepository;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import proposal.MongoDbConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {MongoEmbeddedConfiguration.class, DemoApplication.class}) // add MongoDbConfiguration.class, in order to make this test pass
public class DemoApplicationTests {

    private static final String NICE_SENTENCE = "after convert event NOT received in ";
    
	@Autowired
    private RootRepository rootRepo;
    @Autowired
    private RelatedRepository relatedRepo;

    @Before
    public void setUp() {
        Related embed = new Related(1L, "embed desc");
        Related ref   = new Related(2L, "related desc");
        Related lazy  = new Related(3L, "lazy desc");
        
        relatedRepo.deleteAll();
        embed = relatedRepo.save(embed);
        ref   = relatedRepo.save(ref);
        lazy  = relatedRepo.save(lazy);
                
        rootRepo.deleteAll();
        Root root = new Root(1L, embed, ref, lazy, null, null);
        rootRepo.save(root);
    }
    
    @Test
    public void test() {
        Related relatedOne = relatedRepo.findOne(1L);
        // root content works as expected
        Assert.assertTrue(relatedOne.isConvertEventCalled());
        Assert.assertEquals(1, MongoEventListeener.getAfetLoadEventsCounter());
        
        Root findOne = rootRepo.findOne(1L);
        Assert.assertTrue(findOne.isConvertEventCalled());
        // already embedded document does not trigger anything ... fine
        Assert.assertFalse(NICE_SENTENCE + " embedded content", findOne.getEmbed().isConvertEventCalled());
        
        // this will fail ... no events are fired in DBRef od lazy DBRef contents
        Assert.assertTrue(NICE_SENTENCE + " ref content", findOne.getRef().isConvertEventCalled());
        // should be 3 not 2 because regular DBRef are retrieved eagerly
        Assert.assertEquals(3, MongoEventListeener.getAfetLoadEventsCounter());
        Assert.assertTrue(NICE_SENTENCE + " lazy content", findOne.getLazyRef().isConvertEventCalled());
        Assert.assertEquals(4, MongoEventListeener.getAfetLoadEventsCounter()); //done lazily
    }
}