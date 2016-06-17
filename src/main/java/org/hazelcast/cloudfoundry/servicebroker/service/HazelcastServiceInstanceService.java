/*
 * Copyright (c) 2008-2016, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.hazelcast.cloudfoundry.servicebroker.service;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.hazelcast.cloudfoundry.servicebroker.exception.HazelcastServiceException;
import org.hazelcast.cloudfoundry.servicebroker.repository.HazelcastServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceExistsException;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.DeleteServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.GetLastServiceOperationRequest;
import org.springframework.cloud.servicebroker.model.GetLastServiceOperationResponse;
import org.springframework.cloud.servicebroker.model.ServiceInstance;
import org.springframework.cloud.servicebroker.model.UpdateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.UpdateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.service.ServiceInstanceService;
import org.springframework.stereotype.Service;

import com.hazelcast.core.HazelcastInstance;
/**
 * HazelcastServiceInstanceService class
 */

@Service
public class HazelcastServiceInstanceService implements ServiceInstanceService {

    private static HazelcastServiceRepository repository = HazelcastServiceRepository.getInstance();
    private HazelcastAdmin hazelcastAdmin;

    @Autowired
    public HazelcastServiceInstanceService(HazelcastAdmin hazelcastAdmin) {
        this.hazelcastAdmin = hazelcastAdmin;
    }

    @Override
    public CreateServiceInstanceResponse createServiceInstance(CreateServiceInstanceRequest createServiceInstanceRequest)
             {
        String instanceId = createServiceInstanceRequest.getServiceInstanceId();

        ServiceInstance serviceInstance = repository.findServiceInstance(instanceId);
        if (serviceInstance != null) {
            throw new ServiceInstanceExistsException("error already exists",serviceInstance.toString());
        }

        serviceInstance = new HazelcastServiceInstance(createServiceInstanceRequest);

        HazelcastInstance hazelcastInstance = hazelcastAdmin.createHazelcastInstance(
                createServiceInstanceRequest.getServiceInstanceId());
        if (hazelcastInstance == null) {
            throw new HazelcastServiceException("Failed to create new Hazelcast member hazelcastInstance: "
                    + createServiceInstanceRequest.getServiceInstanceId());
        }

        String hazelcastHost = hazelcastInstance.getCluster().getLocalMember().getAddress().getHost();
        ((HazelcastServiceInstance) serviceInstance).setHazelcastIPAddress(hazelcastHost);

        int hazelcastPort = hazelcastInstance.getCluster().getLocalMember().getAddress().getPort();
        ((HazelcastServiceInstance) serviceInstance).setHazelcastPort(hazelcastPort);

        try {
            InetAddress hazelcastInetAddress = hazelcastInstance.getCluster().getLocalMember().getAddress().getInetAddress();
            ((HazelcastServiceInstance) serviceInstance).setHazelcastInetAddress(hazelcastInetAddress);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        repository.saveServiceInstance(serviceInstance);
        CreateServiceInstanceResponse response=new CreateServiceInstanceResponse();
        
        
        return new CreateServiceInstanceResponse();
    }


    @Override
    public DeleteServiceInstanceResponse deleteServiceInstance(DeleteServiceInstanceRequest deleteServiceInstanceRequest){
        ServiceInstance serviceInstance = repository.findServiceInstance(
                deleteServiceInstanceRequest.getServiceInstanceId());
        if (serviceInstance != null) {
            repository.deleteServiceInstance(serviceInstance);
            hazelcastAdmin.deleteHazelcastInstance(deleteServiceInstanceRequest.getServiceInstanceId());
        }
        return new DeleteServiceInstanceResponse();
    }

    @Override
    public UpdateServiceInstanceResponse updateServiceInstance(UpdateServiceInstanceRequest updateServiceInstanceRequest){
        ServiceInstance serviceInstance = repository.findServiceInstance(updateServiceInstanceRequest.getServiceInstanceId());
        if (serviceInstance == null) {
            throw new ServiceInstanceDoesNotExistException(updateServiceInstanceRequest.getServiceInstanceId());
        }
        repository.deleteServiceInstance(serviceInstance);

        ServiceInstance updatedServiceInstance = new ServiceInstance(updateServiceInstanceRequest);
        repository.saveServiceInstance(updatedServiceInstance);
        return new UpdateServiceInstanceResponse();
    }



	@Override
	public GetLastServiceOperationResponse getLastOperation(GetLastServiceOperationRequest arg0) {
		//TODO check id -> succeeded or failed
		return new GetLastServiceOperationResponse();
	}

}
