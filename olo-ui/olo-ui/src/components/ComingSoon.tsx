/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
export interface ComingSoonProps {

  title: string

  badge?: string

  description?: string

}



const DEFAULT_DESCRIPTION =

  'This area will provide execution monitoring, observability, extensions, and administration features as they ship.'



export function ComingSoon({ title, badge = 'Scheduled', description = DEFAULT_DESCRIPTION }: ComingSoonProps) {

  return (

    <div className="coming-soon">

      <p className="coming-soon-badge">{badge}</p>

      <h2 className="coming-soon-title">{title}</h2>

      <p className="coming-soon-description">{description}</p>

    </div>

  )

}


