import React from 'react';
import './Browse.css';
import axios from "axios";
import Recipe from "../Recipe/Recipe";
import {Col, Row} from "react-bootstrap";
import {RECIPES_URL} from "../../api";


const Browse = () => {

        const [post, setPost] = React.useState(null);

        React.useEffect(() => {
            axios.get(RECIPES_URL).then((response) => {
                setPost(response.data);
            });
        }, []);

        if (!post) return null;

        return (
            <>
                <Row>
                    {post.map((d) => (
                        <Col key={d.id} sm={12} md={6} lg={4} xl={3}>
                            <Recipe id={d.id} title={d.name} description={d.description} image={d.imageUrl} />
                        </Col>
                    ))}
                </Row>
            </>
        );
}


Browse.propTypes = {};

Browse.defaultProps = {};

export default Browse;
