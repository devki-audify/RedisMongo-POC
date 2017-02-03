package com.blog.samples.boot.rest.controller.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import com.blog.samples.Application;
import com.blog.samples.boot.rest.data.DataBuilder;
import com.blog.samples.boot.rest.model.Address;
import com.blog.samples.boot.rest.model.Customer;
import com.blog.samples.boot.rest.repository.CustomerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class, webEnvironment=WebEnvironment.RANDOM_PORT)
public class CustomerControllerIT {
	
    @Autowired
    private TestRestTemplate template;

    @Autowired
    private DataBuilder dataBuilder;

    @Autowired
    private CustomerRepository customerRepository;

	private String base;
    
    private static final String JSON_CONTENT_TYPE = "application/json;charset=UTF-8"; 

    @Before
    public void setUp() throws Exception {
    	this.base = "/rest/customers";
        /* remove and reload test data */
        customerRepository.deleteAll();     
        dataBuilder.createCustomers().forEach(customer -> customerRepository.save(customer));       
    }

    @Test
    public void getAllCustomers() throws Exception {
        ResponseEntity<String> response = template.getForEntity("/rest/customers", String.class);     
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));

        List<Customer> customers = convertJsonToCustomers(response.getBody());      
        assertThat(customers.size(), equalTo(3));       
    }

    @Test
	public void getCustomerById() throws Exception {
		
		Long customerId = getCustomerIdByFirstName("Raja");
		ResponseEntity<String> response = template.getForEntity(String.format("%s/%s", base, customerId), String.class);
		assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
		assertThat(response.getHeaders().getContentType().toString(), equalTo(JSON_CONTENT_TYPE));
		
		Customer customer = convertJsonToCustomer(response.getBody());
		
		assertThat(customer.getFirstName(), equalTo("Raja"));
		assertThat(customer.getLastName(), equalTo("Kolli"));
		assertThat(customer.getDateOfBirth().toString()).isEqualTo("Sun Jan 10 00:00:00 UTC 1982");
		assertThat(customer.getAddress().getStreet(), equalTo("High Street"));
		assertThat(customer.getAddress().getTown(), equalTo("Belfast"));
		assertThat(customer.getAddress().getCounty(), equalTo("India"));
		assertThat(customer.getAddress().getPostcode(), equalTo("BT893PY"));
	}
	
	@Test
	public void createCustomer() throws Exception {

		Customer customer = new Customer("Gary", "Steale", DateTime.parse("1984-03-08").toDate(),
				new Address("Main Street", "Portadown", "Armagh", "BT359JK"));

		ResponseEntity<String> response = template.postForEntity(base, customer, String.class);
		assertThat(response.getStatusCode(), equalTo(HttpStatus.CREATED));
		assertThat(response.getHeaders().getContentType().toString(), equalTo(JSON_CONTENT_TYPE));
		assertThat(response.getHeaders().getFirst("Location"), containsString("/rest/customers/"));
		
		Customer returnedCustomer = convertJsonToCustomer(response.getBody());		
		assertThat(customer.getFirstName(), equalTo(returnedCustomer.getFirstName()));
		assertThat(customer.getLastName(), equalTo(returnedCustomer.getLastName()));
		assertThat(customer.getDateOfBirth(), equalTo(returnedCustomer.getDateOfBirth()));
		assertThat(customer.getAddress().getStreet(), equalTo(returnedCustomer.getAddress().getStreet()));
		assertThat(customer.getAddress().getTown(), equalTo(returnedCustomer.getAddress().getTown()));
		assertThat(customer.getAddress().getCounty(), equalTo(returnedCustomer.getAddress().getCounty()));
		assertThat(customer.getAddress().getPostcode(), equalTo(returnedCustomer.getAddress().getPostcode()));
	}

	@Test
	public void updateCustomer() throws Exception {

		Long customerId = getCustomerIdByFirstName("Raja");
		ResponseEntity<String> getCustomerResponse = template.getForEntity(String.format("%s/%s", base, customerId), String.class);
		assertThat(getCustomerResponse.getStatusCode(), equalTo(HttpStatus.OK));
		assertThat(getCustomerResponse.getHeaders().getContentType().toString(), equalTo(JSON_CONTENT_TYPE));
		
		Customer returnedCustomer = convertJsonToCustomer(getCustomerResponse.getBody());
		assertThat(returnedCustomer.getFirstName(), equalTo("Raja"));
		assertThat(returnedCustomer.getLastName(), equalTo("Kolli"));
		assertThat(returnedCustomer.getDateOfBirth().toString()).isEqualTo("Sun Jan 10 00:00:00 UTC 1982");
		assertThat(returnedCustomer.getAddress().getStreet(), equalTo("High Street"));
		assertThat(returnedCustomer.getAddress().getTown(), equalTo("Belfast"));
		assertThat(returnedCustomer.getAddress().getCounty(), equalTo("India"));
		assertThat(returnedCustomer.getAddress().getPostcode(), equalTo("BT893PY"));
		
		/* convert JSON response to Java and update name */
		ObjectMapper mapper = new ObjectMapper();
		Customer customerToUpdate = mapper.readValue(getCustomerResponse.getBody(), Customer.class);
		customerToUpdate.setFirstName("Wayne");
		customerToUpdate.setLastName("Rooney");

		/* PUT updated customer */
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON); 
		HttpEntity<Customer> entity = new HttpEntity<Customer>(customerToUpdate, headers); 
		ResponseEntity<String> response = template.exchange(String.format("%s/%s", base, customerId), HttpMethod.PUT, entity, String.class, customerId);
		
		assertThat(response.getBody(), nullValue());
		assertThat(response.getStatusCode(), equalTo(HttpStatus.NO_CONTENT));

		/* GET updated customer and ensure name is updated as expected */
		ResponseEntity<String> getUpdatedCustomerResponse = template.getForEntity(String.format("%s/%s", base, customerId), String.class);
		assertThat(getCustomerResponse.getStatusCode(), equalTo(HttpStatus.OK));
		assertThat(getCustomerResponse.getHeaders().getContentType().toString(), equalTo(JSON_CONTENT_TYPE));
		
		Customer updatedCustomer = convertJsonToCustomer(getUpdatedCustomerResponse.getBody());
		assertThat(updatedCustomer.getFirstName(), equalTo("Wayne"));
		assertThat(updatedCustomer.getLastName(), equalTo("Rooney"));
		assertThat(updatedCustomer.getDateOfBirth().toString(), equalTo("Sun Jan 10 00:00:00 IST 1982"));
		assertThat(updatedCustomer.getAddress().getStreet(), equalTo("High Street"));
		assertThat(updatedCustomer.getAddress().getTown(), equalTo("Belfast"));
		assertThat(updatedCustomer.getAddress().getCounty(), equalTo("India"));
		assertThat(updatedCustomer.getAddress().getPostcode(), equalTo("BT893PY"));
	}

	@Test
	public void deleteCustomer() throws Exception {

		Long customerId = getCustomerIdByFirstName("Raja");		
		ResponseEntity<String> response = template.getForEntity(String.format("%s/%s", base, customerId), String.class);
		assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
		assertThat(response.getHeaders().getContentType().toString(), equalTo(JSON_CONTENT_TYPE));
		
		Customer customer = convertJsonToCustomer(response.getBody());
		assertThat(customer.getFirstName(), equalTo("Raja"));
		assertThat(customer.getLastName(), equalTo("Kolli"));
		assertThat(customer.getDateOfBirth().toString()).isEqualTo("Sun Jan 10 00:00:00 UTC 1982");
		assertThat(customer.getAddress().getStreet(), equalTo("High Street"));
		assertThat(customer.getAddress().getTown(), equalTo("Belfast"));
		assertThat(customer.getAddress().getCounty(), equalTo("India"));
		assertThat(customer.getAddress().getPostcode(), equalTo("BT893PY"));
		
		/* delete customer */
		template.delete(String.format("%s/%s", base, customerId), String.class);
		
		/* attempt to get customer and ensure qwe get a 404 */
		ResponseEntity<String> secondCallResponse = template.getForEntity(String.format("%s/%s", base, customerId), String.class);
		assertThat(secondCallResponse.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
	}
	
	@Test
	public void getNonExistantCustomerReturnsError404() throws Exception {
		
		ResponseEntity<String> response = template.getForEntity(String.format("%s/%s", base, 999999), String.class);
		assertThat(response.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));		
	}
	
	/**
	 * Convenience method for testing that gives us the customer 
	 * id based on test customers name. Need this as IDs will increment
	 * as tests are rerun
	 * 
	 * @param firstName
	 * @return customer Id
	 */
	private Long getCustomerIdByFirstName(String firstName){
		return customerRepository.findByFirstName(firstName).stream().findAny().get().getId();
	}
	
	private Customer convertJsonToCustomer(String json) throws Exception {		
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(json, Customer.class);
	}
	
    private List<Customer> convertJsonToCustomers(String json) throws Exception {		
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(json, TypeFactory.defaultInstance().constructCollectionType(List.class, Customer.class));
	}
}