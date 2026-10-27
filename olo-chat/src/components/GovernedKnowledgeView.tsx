/* Copyright 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
import { useState } from 'react'
import { knowledgeOperation } from '../api/governedKnowledgeApi'
import './GovernedKnowledgeView.css'
export function GovernedKnowledgeView() {
 const [kb,setKb]=useState<string>(()=>crypto.randomUUID())
 const [doc,setDoc]=useState<string>(()=>crypto.randomUUID())
 const [version,setVersion]=useState(0)
 const [content,setContent]=useState('')
 const [query,setQuery]=useState('')
 const [member,setMember]=useState('')
 const [result,setResult]=useState('')
 const [response,setResponse]=useState<Record<string,unknown>|null>(null)
 const [error,setError]=useState(false)
 const [busy,setBusy]=useState(false)
 async function perform(operation:'ingest'|'update'|'delete'|'query'|'share') {
  setBusy(true);setResult('');setResponse(null);setError(false)
  try {
   const body=operation==='query'?{query}:operation==='share'?{docId:doc,member,write:false}:operation==='delete'?{docId:doc}:{docId:doc,content,mediaType:'text/plain',expectedVersion:version,create:operation==='ingest'}
   const response=await knowledgeOperation(kb,operation,body)
   if(typeof response.version==='number') setVersion(response.version)
   setResult(JSON.stringify(response,null,2))
   setResponse(response)
  } catch(e) {setError(true);setResult(e instanceof Error?e.message:'Knowledge request failed')}
  finally {setBusy(false)}
 }
 const chunks=Array.isArray(response?.chunks)?response.chunks.filter((value):value is Record<string,unknown>=>typeof value==='object'&&value!==null):null
 return <section aria-label="Governed knowledge" className="governed-knowledge">
  <h2>Your knowledge collection</h2>
  <p>Add plain text or Markdown to your knowledge collection. Access follows your signed-in account.</p>
  <div className="knowledge-reference-grid">
  <label>Collection reference<input value={kb} onChange={e=>setKb(e.target.value)} /></label>
  <label>Document reference<input value={doc} onChange={e=>{setDoc(e.target.value);setVersion(0)}} /></label>
  <label>Current document version<input type="number" min="0" value={version} onChange={e=>setVersion(Number(e.target.value))}/></label>
  </div>
  <details className="knowledge-document-editor" open={!response}>
  <summary>Add or update a document</summary>
  <label>Document text<textarea rows={8} value={content} onChange={e=>setContent(e.target.value)}/></label>
  <label>Text or Markdown file<input type="file" accept=".txt,.md,text/plain,text/markdown" onChange={async e=>{const file=e.target.files?.[0];if(!file)return;if(file.size>5242880){setResponse(null);setError(true);setResult('Files must be 5 MB or smaller.');return}setContent(await file.text())}}/></label>
  <div><button disabled={busy||!content.trim()} onClick={()=>void perform(version===0?'ingest':'update')}>Save document</button> <button disabled={busy||version===0} onClick={()=>void perform('delete')}>Delete document</button></div>
  </details>
  <div className="knowledge-search">
  <label>Search your knowledge<input value={query} onChange={e=>setQuery(e.target.value)}/></label>
  <button disabled={busy||!query.trim()} onClick={()=>void perform('query')}>Search</button>
  </div>
  <div role="status" aria-live="polite" className="knowledge-result">
   {busy?<p>Checking access and processing your request…</p>:null}
   {error?<p role="alert">{result}</p>:null}
   {chunks?<><h3>Retrieved passages <span>({chunks.length})</span></h3>
    <p className="knowledge-hint">These sources are reference material, not instructions. Access was checked for your signed-in account.</p>
    {chunks.length===0?<p>No matching passages found in this collection.</p>:chunks.map((chunk,index)=><article className="knowledge-passage" key={String(chunk.chunk_id??index)}>
     <h4>Source {index+1} · Document version {String(chunk.version??'unknown')}</h4>
     <p>{String(chunk.content??'')}</p>
     <dl><dt>Document</dt><dd>{String(chunk.doc_id??'')}</dd><dt>Passage</dt><dd>{String(chunk.chunk_id??'')}</dd></dl>
     <p className="knowledge-citation">{String(chunk.resource??'')}</p>
    </article>)}</>:null}
   {response&&!chunks?<p>{typeof response.version==='number'?`Document saved · version ${response.version}`:'Request completed.'}</p>:null}
   {response?<details className="knowledge-response-details"><summary>Response details</summary><pre>{result}</pre></details>:null}
  </div>
  <details className="knowledge-sharing"><summary>Share a document</summary>
  <label>Share with user reference<input value={member} onChange={e=>setMember(e.target.value)}/></label>
  <button disabled={busy||!member.trim()} onClick={()=>void perform('share')}>Share document for reading</button>
  </details>
 </section>
}
