package ja.tools.storage.dto;

import java.util.Date;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class StoredItem {
	private String name;
	private Date created;
	private Date lastModified;
	private Long contentLength;
	private Object itemRef;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public Date getLastModified() {
		return lastModified;
	}

	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

	public Long getContentLength() {
		return contentLength;
	}

	public void setContentLength(Long contentLength) {
		this.contentLength = contentLength;
	}

	public Object getItemRef() {
		return itemRef;
	}

	public void setItemRef(Object itemRef) {
		this.itemRef = itemRef;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append(name).append(created).append(lastModified).build();
	}
}
