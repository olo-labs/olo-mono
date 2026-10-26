/* Copyright 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
import { CHAT_API, withAuth } from './chatApiClient'
import type { RunEventDto } from './chatTypes'
/** Poll only governed metadata. Each poll reauthorizes; private event payloads are never streamed. */
export function streamRunEvents(runId:string,onEvent:(event:RunEventDto)=>void,onError?:(err:unknown)=>void):()=>void {
 const controller=new AbortController()
 let timer:ReturnType<typeof setTimeout>|undefined
 async function poll() {
  try {
   const res=await fetch(`${CHAT_API}/v1/runs/${encodeURIComponent(runId)}`,{headers:withAuth(),signal:controller.signal})
   if(!res.ok) throw new Error(`Run authorization or status unavailable: ${res.status}`)
   const {status}=await res.json()
   if(controller.signal.aborted) return
   if(status==='running'||status==='waiting_human') { timer=setTimeout(poll,1500); return }
   onEvent({runId,nodeId:'root',parentNodeId:null,nodeType:'SYSTEM',status:status==='completed'?'COMPLETED':'FAILED',timestamp:Date.now(),metadata:{phase:'kernel-result'},output:status==='cancelled'?{status:'CANCELLED'}:undefined})
  } catch(error) {if(!controller.signal.aborted) onError?.(error)}
 }
 void poll()
 return ()=>{controller.abort();if(timer) clearTimeout(timer)}
}
