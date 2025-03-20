package com.healthCheck.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.healthCheck.model.File;

public interface FileRepository extends JpaRepository<File, String>{

}
