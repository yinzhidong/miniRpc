package minirpc.service;

public class TestServiceImpl implements TestService {

	@Override
	public String say(String what) {
		return "testServiceImpl--" + what ;
	}

	@Override
	public int add(int a, int b) {
		return a + b;
	}

}
