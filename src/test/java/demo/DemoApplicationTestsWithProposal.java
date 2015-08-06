package demo;

import demo.model.Related;
import demo.model.Root;
import proposal.MongoDbConfiguration;
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

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {MongoEmbeddedConfiguration.class, MongoDbConfiguration.class, DemoApplication.class})
public class DemoApplicationTestsWithProposal {

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
        Root root = new Root(1L, embed, ref, lazy, Arrays.asList(ref, lazy), Arrays.asList(ref, lazy));
        rootRepo.save(root);
    }
    
    @Test
    public void test() {
        Related relatedOne = relatedRepo.findOne(1L);
        Assert.assertTrue(relatedOne.isConvertEventCalled());
        Assert.assertEquals(1, MongoEventListeener.getAfetLoadEventsCounter());
        
        Root findOne = rootRepo.findOne(1L);
        Assert.assertTrue(findOne.isConvertEventCalled());
        // 5 because direct DBRef is eagerly fetched, and also the list of two DBRef is now also eagerly fetched
        Assert.assertEquals(5, MongoEventListeener.getAfetLoadEventsCounter());
        // already embedded document does not trigger anything ... fine
        Assert.assertFalse(NICE_SENTENCE + " embedded content", findOne.getEmbed().isConvertEventCalled());
        Assert.assertEquals(5, MongoEventListeener.getAfetLoadEventsCounter());
        // dbref eager fetched, nothing new is fired ... fine
        Assert.assertTrue(NICE_SENTENCE + " ref content", findOne.getRef().isConvertEventCalled());
        Assert.assertEquals(5, MongoEventListeener.getAfetLoadEventsCounter());
        // lazyDbRef fetched when accessing to it ... so the counter will increment
        Assert.assertTrue(NICE_SENTENCE + " lazy content", findOne.getLazyRef().isConvertEventCalled());
        Assert.assertEquals(6, MongoEventListeener.getAfetLoadEventsCounter());
        
        // dealing with lists (ensuring all have received the callback)
        Assert.assertEquals(0, findOne.getListRef().stream().filter(rel -> !rel.isConvertEventCalled()).count());
        // list of dbRef already eagerly fetched
        Assert.assertEquals(6, MongoEventListeener.getAfetLoadEventsCounter());
        Assert.assertEquals(0, findOne.getListLazy().stream().filter(rel -> !rel.isConvertEventCalled()).count());
        // now we have just fetched lazy relations contained in the list
        Assert.assertEquals(8, MongoEventListeener.getAfetLoadEventsCounter());
    }
}