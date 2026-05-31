package org.exoplatform.mfa.storage.entity;

import io.meeds.common.persistence.PortableSequence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity(name = "RevocationRequestEntity")
@Table(name = "MFA_REVOCATION_REQUEST")
@NamedQuery(name = "RevocationRequestEntity.countByUsernameAndType", query = "SELECT count(*) "
    + "FROM RevocationRequestEntity revocationRequest "
    + "WHERE revocationRequest.username = :username "
    + "AND revocationRequest.type = :type")
@NamedQuery(name = "RevocationRequestEntity.deleteByUsernameAndType", query = "DELETE "
    + "FROM RevocationRequestEntity revocationRequest "
    + "WHERE revocationRequest.username = :username "
    + "AND revocationRequest.type = :type")
@NamedQuery(name = "RevocationRequestEntity.deleteById", query = "DELETE "
    + "FROM RevocationRequestEntity revocationRequest "
    + "WHERE revocationRequest.id = :id")
@NamedQuery(name = "RevocationRequestEntity.findById", query = "SELECT revocationRequest "
    + "FROM RevocationRequestEntity revocationRequest "
    + "WHERE revocationRequest.id = :id")
public class RevocationRequestEntity {

  @Id
  @PortableSequence(name = "SEQ_REVOCATION_REQUEST_ID")
  @Column(name = "ID")
  private Long id;

  @Column(name = "USERNAME", nullable = false)
  private String username;

  @Column(name = "TYPE")
  private String type;

  public RevocationRequestEntity(Long id, String username, String type) {
    this.id = id;
    this.username = username;
    this.type = type;
  }

  public RevocationRequestEntity() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }
}
