objectstore
===========

Composent Object Store 

The ObjectStore is an OSGi-based set of services and APIs to allow java objects to be stored and retrieved to/from a CQL3-based store (Cassandra 2+).   The API is very small/simple and does not use object-relational mapping (ORM).

Example API Usage

Given an injected instance of IObjectStore:

IObjectStore store = <injected via OSGi declarative services>

Example 

StoreObject storeObject = store.createStoreObject("MyObjectClass");
// add data
storeObject.put("username","scottslewis");
storeObject.put("age",10);
// store/persist
storeObject.store();

Retrieval

StoreObjectQuery query = store.createQuery("MyObjectClass");
// qualify to only get where age = 10
query.setWhere(new Where(new Relation("age",Op.EQ,new Value(10)));
// execute query
Collection<StoreObject> storeObjects = query.execute();
// Show objects found
for(StoreObject so: storeObjects) {
    System.out.println("id="+so.getId());
    System.out.println("username="+so.getString("username"));
    System.out.println("age="+so.getInteger("age"));
}

StoreObject (com.composent.objectstore.StoreObject) supports a variety of primative types (String, Integer, Long, Float, Double, byte[], UUID) as well as collection types (Set, List, Map). 

I'm working on/extending the StoreObjectQuery to be simpler, more expressive, and hopefully more compact.

Also supported are CQL features for controlling consistency level and lightweight transactions are supported at StoreObject level.

There is also a Storeable class (com.composent.objectstore) that allows subclasses that can directly control their storage and revival by overriding superclass methods...for example:

public MyData extends Storable {

private String name;
private Integer age;

public MyData() {
    super("MyObjectClass"):
}

public MyData(String name, Integer age) {
    this.name = name;
    this.age = age;
}

protected void storeFields(StoreObject storeObject) throws StoreException {
	storePrimitiveField(storeObject, "name", this.name);
	storePrimitiveField(storeObject, "name", this.age);
}

protected void reviveFields(StoreObject storeObject) throws StoreException {
	this.name = revivePrimitiveField(storeObject,String.class);
	this.age = revivePrimitiveField(storeObject,Integer.class);
}

}

Then instances can be stored via:

myData.storeTo(store);

and revived by:

Collection<MyData> objects = (Collection<MyData>) new MyData().reviveAll(store);

Both/either of the StoreObject and/or the Storable APIs may be used to store and retrieve objects via CQL.  

Until docs, examples, and tests have been created, please see the source code for IObjectStore, StoreObject, and Storable classes for more details about the API.

