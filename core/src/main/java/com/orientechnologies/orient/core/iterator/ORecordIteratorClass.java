/*
 *
 *  *  Copyright 2014 Orient Technologies LTD (info(at)orientechnologies.com)
 *  *
 *  *  Licensed under the Apache License, Version 2.0 (the "License");
 *  *  you may not use this file except in compliance with the License.
 *  *  You may obtain a copy of the License at
 *  *
 *  *       http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *  Unless required by applicable law or agreed to in writing, software
 *  *  distributed under the License is distributed on an "AS IS" BASIS,
 *  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  See the License for the specific language governing permissions and
 *  *  limitations under the License.
 *  *
 *  * For more information: http://www.orientechnologies.com
 *
 */
package com.orientechnologies.orient.core.iterator;

import java.util.Arrays;

import com.orientechnologies.orient.core.db.ODatabaseDocumentInternal;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClassImpl;
import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.storage.OStorage;

/**
 * Iterator class to browse forward and backward the records of a cluster. Once browsed in a direction, the iterator cannot change
 * it. This iterator with "live updates" set is able to catch updates to the cluster sizes while browsing. This is the case when
 * concurrent clients/threads insert and remove item in any cluster the iterator is browsing. If the cluster are hot removed by from
 * the database the iterator could be invalid and throw exception of cluster not found.
 * 
 * @author Luca Garulli
 */
public class ORecordIteratorClass<REC extends ORecord> extends ORecordIteratorClusters<REC> {
  protected final OClass targetClass;
  protected boolean      polymorphic;

  /**
   * This method is only to maintain the retro compatibility with TinkerPop BP 2.2
   */
  public ORecordIteratorClass(final ODatabaseDocumentInternal iDatabase, final ODatabaseDocumentTx iLowLevelDatabase,
      final String iClassName, final boolean iPolymorphic) {
    this(iDatabase, iLowLevelDatabase, iClassName, iPolymorphic, true, false);
  }

  public ORecordIteratorClass(final ODatabaseDocumentInternal iDatabase, final ODatabaseDocumentInternal iLowLevelDatabase,
      final String iClassName, final boolean iPolymorphic) {
    this(iDatabase, iLowLevelDatabase, iClassName, iPolymorphic, true, false);
  }

  public ORecordIteratorClass(final ODatabaseDocumentInternal iDatabase, final ODatabaseDocumentInternal iLowLevelDatabase,
      final String iClassName, final boolean iPolymorphic, final boolean iUseCache, final boolean iterateThroughTombstones) {
    this(iDatabase, iLowLevelDatabase, iClassName, iPolymorphic, iUseCache, iterateThroughTombstones,
        OStorage.LOCKING_STRATEGY.DEFAULT);
  }

  public ORecordIteratorClass(final ODatabaseDocumentInternal iDatabase, final ODatabaseDocumentInternal iLowLevelDatabase,
      final String iClassName, final boolean iPolymorphic, final boolean iUseCache, final boolean iterateThroughTombstones,
      final OStorage.LOCKING_STRATEGY iLockingStrategy) {
    super(iDatabase, iLowLevelDatabase, iUseCache, iterateThroughTombstones, iLockingStrategy);

    targetClass = database.getMetadata().getImmutableSchemaSnapshot().getClass(iClassName);
    if (targetClass == null)
      throw new IllegalArgumentException("Class '" + iClassName + "' was not found in database schema");

    polymorphic = iPolymorphic;
    clusterIds = polymorphic ? targetClass.getPolymorphicClusterIds() : targetClass.getClusterIds();
    clusterIds = OClassImpl.readableClusters(iDatabase, clusterIds);

    Arrays.sort(clusterIds);

    config();
  }

  @SuppressWarnings("unchecked")
  @Override
  public REC next() {
    final OIdentifiable rec = super.next();
    if (rec == null)
      return null;
    return (REC) rec.getRecord();
  }

  @SuppressWarnings("unchecked")
  @Override
  public REC previous() {
    final OIdentifiable rec = super.previous();
    if (rec == null)
      return null;

    return (REC) rec.getRecord();
  }

  @Override
  protected boolean include(final ORecord record) {
    return record instanceof ODocument && targetClass.isSuperClassOf(((ODocument) record).getImmutableSchemaClass());
  }

  public boolean isPolymorphic() {
    return polymorphic;
  }

  @Override
  public String toString() {
    return String.format("ORecordIteratorClass.targetClass(%s).polymorphic(%s)", targetClass, polymorphic);
  }
}
