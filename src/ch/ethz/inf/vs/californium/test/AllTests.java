package ch.ethz.inf.vs.californium.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ DatagramReadWriteTest.class, MessageTest.class,
		OptionTest.class, RequestTest.class, ResourcesTest.class,
		ResourceTest.class, TokenEqualityTest.class, CoapProxyTest.class })
public class AllTests {
	
}
