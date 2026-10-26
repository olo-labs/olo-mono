/* Copyright 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
import { CHAT_API, withAuth } from './chatApiClient'
export async function knowledgeCollections():Promise<string[]> {
 const res=await fetch(`${CHAT_API}/v1/knowledge`,{headers:withAuth()})
 if(!res.ok) throw new Error(`Knowledge authorization unavailable: ${res.status}`)
 return res.json()
}
export async function knowledgeOperation(kb:string,operation:'ingest'|'update'|'delete'|'query'|'share',body:Record<string,unknown>):Promise<Record<string,unknown>> {
 const res=await fetch(`${CHAT_API}/v1/knowledge/${encodeURIComponent(kb)}/${operation}`,{method:'POST',headers:withAuth({'Content-Type':'application/json','Idempotency-Key':crypto.randomUUID()}),body:JSON.stringify(body)})
 if(!res.ok) throw new Error(`Knowledge request failed: ${res.status}`)
 return res.json()
}
