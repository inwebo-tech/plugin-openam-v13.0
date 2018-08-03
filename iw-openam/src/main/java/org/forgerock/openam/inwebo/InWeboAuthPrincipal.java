package org.forgerock.openam.inwebo;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;
import java.security.Principal;

/**
 * Authorization Principal for InWebo Security.
 * This is required by the authorization interface.
 */
public class InWeboAuthPrincipal implements Principal, Serializable {

  private static final long serialVersionUID = -4375241626363474200L;

  private final String name;

  public InWeboAuthPrincipal(final String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  @Override
  public boolean equals(final Object obj) {
    return EqualsBuilder.reflectionEquals(this, obj);
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this);
  }
}
