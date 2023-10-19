package com.qwery.runtime.devices

import com.qwery.language.models.Queryable
import com.qwery.runtime.DatabaseManagementSystem.{getUpdatedTime, readVirtualTable}
import com.qwery.runtime.{DatabaseObjectNS, DatabaseObjectRef, QweryVM, ROWID, Scope}
import com.qwery.util.LogUtil
import qwery.io.IOCost

/**
 * Represents a Virtual Table Row Collection (e.g. view)
 * @param queryable the query that represents the source of data
 * @param host      the [[RowCollection materialized device]]
 */
class VirtualTableRowCollection(val queryable: Queryable,
                                val host: RowCollection,
                                val dependencies: Seq[DatabaseObjectNS])
  extends HostedRowCollection with ReadOnlyRecordCollection[Row] {
  private var lastCheck: Long = 0
  private val scope = Scope()

  override def apply(rowID: ROWID): Row = {
    rebuildIfUpdated()
    super.apply(rowID)
  }

  def rebuild(): IOCost = {
    LogUtil(this).info(s"$ns: rebuilding view...")
    host.setLength(0)
    val (_, cost0, result1) = QweryVM.search(scope, queryable)
    val cost1 = host.insert(result1)
    val cost2 = host match {
      case rc: IndexedRowCollection => rc.rebuild()
      case _ => IOCost()
    }
    lastCheck = System.currentTimeMillis()
    cost0 ++ cost1 ++ cost2
  }

  def rebuildIfUpdated(): IOCost = {
    if ((System.currentTimeMillis() - lastCheck >= 30000L) && isOutOfSync) {
      lastCheck = System.currentTimeMillis()
      rebuild()
    } else IOCost()
  }

  def isOutOfSync: Boolean = {
    val myUpdatedTime = getUpdatedTime(ns)
    val depUpdatedTimes = dependencies.map(getUpdatedTime)
    depUpdatedTimes.exists(_ >= myUpdatedTime)
  }

}

/**
 * Virtual Table File Companion
 */
object VirtualTableRowCollection {

  /**
   * Retrieves a virtual table by name
   * @param ref   the [[DatabaseObjectRef]]
   * @param scope the implicit [[Scope scope]]
   * @return the [[RowCollection virtual table]]
   */
  def apply(ref: DatabaseObjectRef)(implicit scope: Scope): RowCollection = readVirtualTable(ref.toNS)

}