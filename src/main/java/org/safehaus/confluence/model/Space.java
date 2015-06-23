package org.safehaus.confluence.model;


public class Space implements ConfluenceEntity
{
	private String id;
	private String key;
	private String name;
	private String type;

	private Description description;


	public String getId()
	{
		return id;
	}


	public void setId( String id )
	{
		this.id = id;
	}


	public String getKey()
	{
		return key;
	}


	public void setKey( String key )
	{
		this.key = key;
	}


	public String getName()
	{
		return name;
	}


	public void setName( String name )
	{
		this.name = name;
	}


	public String getType()
	{
		return type;
	}


	public void setType( String type )
	{
		this.type = type;
	}


	public Description getDescription()
	{
		return description;
	}


	public void setDescription( Description description )
	{
		this.description = description;
	}

	public class Description
	{
		private Value plain;


		public Value getPlain()
		{
			return plain;
		}


		public void setPlain( Value plain )
		{
			this.plain = plain;
		}

		public class Value
		{
			private String value;
			private String representation;


			public String getValue()
			{
				return value;
			}


			public void setValue( String value )
			{
				this.value = value;
			}


			public String getRepresentation()
			{
				return representation;
			}


			public void setRepresentation( String representation )
			{
				this.representation = representation;
			}

		}
	}
}
